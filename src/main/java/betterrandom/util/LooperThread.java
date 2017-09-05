package betterrandom.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * <p> EXPERIMENTAL. Thread that loops a given task until interrupted (or until JVM shutdown, if it
 * {@link #isDaemon()}), with the iterations being transactional. Because of these constraints, it
 * can be serialized and cloned. Subclasses must override {@link #iterate()}; the only reason this
 * class is concrete is that temporary instances are needed during deserialization. The loop should
 * be reasonably short, since it will block serialization and cloning that would otherwise catch it
 * in mid-iteration. </p><p> Thread state that WILL be restored includes: </p> <ul> <li>{@link
 * #getName()}</li> <li>{@link #getPriority()}</li> <li>{@link #getState()} == {@link
 * State#NEW}</li> <li>{@link #getState()} == {@link State#TERMINATED}</li> <li>{@link
 * #isInterrupted()}</li> <li>{@link #isDaemon()}</li> </ul><p> Thread state that will be restored
 * ONLY if its values are {@link Serializable} includes: </p><ul> <li>{@link #getThreadGroup()}</li>
 * <li>{@link #getUncaughtExceptionHandler()}</li> <li>{@link #getContextClassLoader()}</li>
 * </ul><p> Thread state that will NEVER be restored includes: </p><ul> <li>Program counter, call
 * stack, and local variables. The thread will restart.</li> <li>Suspended status (see {@link
 * Thread#suspend()}</li> <li>{@link #getState()} == {@link State#TIMED_WAITING}</li> <li>{@link
 * #getState()} == {@link State#WAITING}</li> <li>{@link #getState()} == {@link State#BLOCKED}</li>
 * <li>{@link #getId()}</li> <li>{@link #holdsLock(Object)}</li> </ul>
 */
public class LooperThread extends Thread implements Serializable, Cloneable {

  private static final LogPreFormatter LOG = new LogPreFormatter(LooperThread.class);
  private static final long serialVersionUID = -4387051967625864310L;

  private static class ThreadDeathAlreadyHandled extends ThreadDeath {

    private static final long serialVersionUID = 1433257085225877245L;
  }

  private static class UncaughtExceptionHandlerWrapper
      implements UncaughtExceptionHandler, Serializable {

    private static final long serialVersionUID = -132520277274461153L;
    @Nullable
    private UncaughtExceptionHandler wrapped = null;

    @SuppressWarnings("deprecation")
    @Override
    public void uncaughtException(Thread t, Throwable e) {
      if (!(e instanceof ThreadDeathAlreadyHandled)) {
        LOG.error("Uncaught exception: %s", e);
        if (wrapped != null) {
          wrapped.uncaughtException(t, e);
        } else {
          UncaughtExceptionHandler defaultHandler = getDefaultUncaughtExceptionHandler();
          if (defaultHandler == null) {
            e.printStackTrace();
            t.stop(e); // necessary so that join() (which is final) will return
          } else {
            defaultHandler.uncaughtException(t, e);
          }
        }
      }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
      wrapped = serializableOrNull(wrapped);
      out.defaultWriteObject();
    }
  }

  /**
   * The thread holds this lock whenever it is being serialized or cloned or is running
   * {@link #iterate()} called by {@link #run()}.
   */
  protected transient Lock lock = new ReentrantLock();

  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  private transient boolean alreadyTerminatedWhenDeserialized = false;
  private boolean interrupted = false;
  private boolean daemon = false;
  private int priority = Thread.NORM_PRIORITY;
  private State state = State.NEW;
  @Nullable
  private ThreadGroup group;
  private final UncaughtExceptionHandlerWrapper uncaughtExceptionHandler =
      new UncaughtExceptionHandlerWrapper();
  @Nullable
  private ClassLoader contextClassLoader = null;
  @MonotonicNonNull
  private String name = null;

  private void setGroup(@UnderInitialization LooperThread this, @org.jetbrains.annotations.Nullable ThreadGroup group) {
    if (uncaughtExceptionHandler.wrapped == null) {
      uncaughtExceptionHandler.wrapped = group;
    }
  }

  public LooperThread() {
    super();
  }

  public LooperThread(String name) {
    super(name);
  }

  public LooperThread(ThreadGroup group, String name) {
    super(group, name);
    setGroup(group);
  }

  @Nullable
  private static <T> T serializableOrNull(@Nullable T object) {
    if (!(object instanceof Serializable)) {
      return null;
    }
    return object;
  }

  /**
   * Used only to prepare subclasses before readResolve.
   */
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    lock = new ReentrantLock();
  }

  /**
   * Use readResolve rather than readObject, because stack size and thread group can only be
   * restored in Thread constructors.
   *
   * @return A LooperThread that will replace this one during deserialization.
   */
  @SuppressWarnings("deprecation")
  protected Object readResolve() {
    LooperThread t;
    ThreadGroup group = this.group;
    if (group != null) {
      if (name == null) {
        name = getName();
      }
      t = new LooperThread(group, name);
    } else {
      if (name == null) {
        t = new LooperThread();
      } else {
        t = new LooperThread(name);
      }
    }
    t.setDaemon(daemon);
    if (name != null) {
      t.setName(name);
    }
    t.setPriority(priority);
    if (uncaughtExceptionHandler.wrapped != null) {
      t.setUncaughtExceptionHandler(uncaughtExceptionHandler.wrapped);
    }
    if (contextClassLoader != null) {
      t.setContextClassLoader(contextClassLoader);
    }
    switch (state) {
      case NEW:
        t.alreadyTerminatedWhenDeserialized = false;
        break;
      case RUNNABLE:
      case BLOCKED:
      case WAITING:
      case TIMED_WAITING:
        t.alreadyTerminatedWhenDeserialized = false;
        t.start();
        break;
      case TERMINATED:
        t.stop(new ThreadDeathAlreadyHandled());
        t.alreadyTerminatedWhenDeserialized = true;
    }
    if (interrupted) {
      t.interrupt();
    }
    return t;
  }

  /**
   * The task that will be iterated indefinitely.
   */
  protected void iterate() throws InterruptedException {
    throw new UnsupportedOperationException("This method should be overridden!");
  }

  @Override
  public final void run() {
    while (true) {
      try {
        lock.lockInterruptibly();
        try {
          iterate();
        } finally {
          lock.unlock();
        }
      } catch (InterruptedException e) {
        LOG.error("Interrupted: %s", e);
        interrupt();
        break;
      }
    }
  }

  @Override
  public void setUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
    uncaughtExceptionHandler.wrapped = handler;
    super.setUncaughtExceptionHandler(uncaughtExceptionHandler);
  }

  @SuppressWarnings("override.return.invalid")
  @Nullable
  @Override
  public UncaughtExceptionHandler getUncaughtExceptionHandler() {
    return uncaughtExceptionHandler.wrapped;
  }

  @Override
  public State getState() {
    if (alreadyTerminatedWhenDeserialized) {
      return State.TERMINATED;
    } else {
      return super.getState();
    }
  }

  @Override
  public void start() {
    if (alreadyTerminatedWhenDeserialized) {
      throw new IllegalThreadStateException(
          "This thread was deserialized from one that had already terminated");
    }
    super.start();
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    lock.lock();
    try {
      interrupted = isInterrupted();
      daemon = isDaemon();
      name = getName();
      priority = getPriority();
      state = getState();
      group = serializableOrNull(getThreadGroup());
      contextClassLoader = serializableOrNull(getContextClassLoader());
      out.defaultWriteObject();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public LooperThread clone() {
    lock.lock();
    try (
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
      objectOutputStream.writeObject(this);
      try (
          ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
              byteArrayOutputStream.toByteArray());
          ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
        return (LooperThread) objectInputStream.readObject();
      }
    } catch (IOException | ClassNotFoundException e) {
      throw new InternalError(e);
    } finally {
      lock.unlock();
    }
  }
}
