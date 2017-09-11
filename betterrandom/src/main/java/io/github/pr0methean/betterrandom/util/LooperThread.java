package io.github.pr0methean.betterrandom.util;

import static org.checkerframework.checker.nullness.NullnessUtil.castNonNull;

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
 * ONLY if its values are {@link java.io.Serializable} includes: </p><ul> <li>{@link
 * #getThreadGroup()}</li> <li>{@link #getUncaughtExceptionHandler()}</li> <li>{@link
 * #getContextClassLoader()}</li> </ul><p> Thread state that will NEVER be restored includes:
 * </p><ul> <li>Program counter, call stack, and local variables. The seederThread will
 * restart.</li> <li>Suspended status (see {@link Thread#suspend()}</li> <li>{@link #getState()} ==
 * {@link State#TIMED_WAITING}</li> <li>{@link #getState()} == {@link State#WAITING}</li> <li>{@link
 * #getState()} == {@link State#BLOCKED}</li> <li>{@link #getId()}</li> <li>{@link
 * #holdsLock(Object)}</li> </ul>
 *
 * @author ubuntu
 * @version $Id: $Id
 */
public class LooperThread extends Thread implements Serializable, Cloneable {

  private static final LogPreFormatter LOG = new LogPreFormatter(LooperThread.class);
  private static final long serialVersionUID = -4387051967625864310L;
  private final UncaughtExceptionHandlerWrapper uncaughtExceptionHandler =
      new UncaughtExceptionHandlerWrapper();
  /**
   * The seederThread holds this lock whenever it is being serialized or cloned or is running {@link
   * #iterate()} called by {@link #run()}.
   */
  protected transient Lock lock = new ReentrantLock();
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  private transient boolean alreadyTerminatedWhenDeserialized = false;
  private boolean interrupted = false;
  private boolean daemon = false;
  private int priority = Thread.NORM_PRIORITY;
  private State state = State.NEW;
  private @Nullable ThreadGroup group;
  private @Nullable ClassLoader contextClassLoader = null;
  private @MonotonicNonNull String name = null;

  /**
   * <p>Constructor for LooperThread.</p>
   */
  public LooperThread() {
    super();
  }

  /**
   * <p>Constructor for LooperThread.</p>
   *
   * @param name a {@link java.lang.String} object.
   */
  public LooperThread(final String name) {
    super(name);
  }

  /**
   * <p>Constructor for LooperThread.</p>
   *
   * @param group a {@link java.lang.ThreadGroup} object.
   * @param name a {@link java.lang.String} object.
   */
  public LooperThread(final ThreadGroup group, final String name) {
    super(group, name);
    setGroup(group);
  }

  private static @Nullable <T> T serializableOrNull(final @Nullable T object) {
    if (!(object instanceof Serializable)) {
      return null;
    }
    return object;
  }

  private void setGroup(@UnderInitialization LooperThread this,
      @org.jetbrains.annotations.Nullable final ThreadGroup group) {
    if (uncaughtExceptionHandler.wrapped == null) {
      uncaughtExceptionHandler.wrapped = group;
    }
  }

  /**
   * Used only to prepare subclasses before readResolve.
   *
   * @param in The {@link ObjectInputStream} we're being read from.
   * @throws IOException When thrown by {@link ObjectInputStream#defaultReadObject}.
   * @throws ClassNotFoundException When thrown by {@link ObjectInputStream#defaultReadObject}.
   */
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    lock = new ReentrantLock();
  }

  /**
   * Use readResolve rather than readObject, because stack size and seederThread group can only be
   * restored in Thread constructors.
   *
   * @return A LooperThread that will replace this one during deserialization.
   */
  @SuppressWarnings("deprecation")
  protected Object readResolve() {
    final LooperThread t;
    if (group != null) {
      t = new LooperThread(castNonNull(group), name == null ? getName() : name);
    } else {
      t = name == null ? new LooperThread() : new LooperThread(name);
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
   *
   * @throws java.lang.InterruptedException if any.
   */
  protected void iterate() throws InterruptedException {
    throw new UnsupportedOperationException("This method should be overridden!");
  }

  /** {@inheritDoc} */
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
      } catch (final InterruptedException e) {
        LOG.error("Interrupted: %s", e);
        interrupt();
        break;
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  @SuppressWarnings("override.return.invalid")
  public @Nullable UncaughtExceptionHandler getUncaughtExceptionHandler() {
    return uncaughtExceptionHandler.wrapped;
  }

  /** {@inheritDoc} */
  @Override
  public void setUncaughtExceptionHandler(final UncaughtExceptionHandler handler) {
    uncaughtExceptionHandler.wrapped = handler;
    super.setUncaughtExceptionHandler(uncaughtExceptionHandler);
  }

  /** {@inheritDoc} */
  @Override
  public State getState() {
    return alreadyTerminatedWhenDeserialized ? State.TERMINATED : super.getState();
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void start() {
    if (alreadyTerminatedWhenDeserialized) {
      throw new IllegalThreadStateException(
          "This seederThread was deserialized from one that had already terminated");
    }
    super.start();
  }

  private void writeObject(final ObjectOutputStream out) throws IOException {
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

  /** {@inheritDoc} */
  @SuppressWarnings("MethodDoesntCallSuperMethod")
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

  private static class ThreadDeathAlreadyHandled extends ThreadDeath {

    private static final long serialVersionUID = 1433257085225877245L;
  }

  private static class UncaughtExceptionHandlerWrapper
      implements UncaughtExceptionHandler, Serializable {

    private static final long serialVersionUID = -132520277274461153L;
    private @Nullable UncaughtExceptionHandler wrapped = null;

    @SuppressWarnings("deprecation")
    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
      if (!(e instanceof ThreadDeathAlreadyHandled)) {
        LOG.error("Uncaught exception: %s", e);
        if (wrapped != null) {
          wrapped.uncaughtException(t, e);
        } else {
          final UncaughtExceptionHandler defaultHandler = getDefaultUncaughtExceptionHandler();
          if (defaultHandler == null) {
            e.printStackTrace();
            t.stop(e); // necessary so that join() (which is final) will return
          } else {
            defaultHandler.uncaughtException(t, e);
          }
        }
      }
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
      wrapped = serializableOrNull(wrapped);
      out.defaultWriteObject();
    }
  }
}
