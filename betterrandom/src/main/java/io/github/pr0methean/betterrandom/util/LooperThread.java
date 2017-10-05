package io.github.pr0methean.betterrandom.util;

import static org.checkerframework.checker.nullness.NullnessUtil.castNonNull;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/**
 * <p>Thread that loops a given task until interrupted (or until JVM shutdown, if it {@link
 * #isDaemon() is a daemon thread}), with the iterations being transactional. Because of these
 * constraints, it can be serialized and cloned. Subclasses must override {@link #iterate()} if
 * instantiated without a target {@link Runnable}; the only reason this class is concrete is that
 * temporary instances are needed during deserialization.</p> <p> Subclasses should override the
 * {@link #readResolveConstructorWrapper()} method to ensure they are deserialized as a subclass
 * instance. </p> <p>{@link #iterate()}'s body should be reasonably short, since it will block
 * serialization and cloning that would otherwise catch it in mid-iteration. </p><p> Thread state
 * that WILL be restored includes that retrievable by: </p> <ul> <li>{@link #getName()}</li>
 * <li>{@link #getPriority()}</li> <li>{@link #getState()} == {@link State#NEW}</li> <li>{@link
 * #getState()} == {@link State#TERMINATED}</li> <li>{@link #isInterrupted()}</li> <li>{@link
 * #isDaemon()}</li> </ul><p> Thread state that will be restored ONLY if its values are {@link
 * Serializable} includes that retrievable by: </p><ul> <li>{@link #getThreadGroup()}</li>
 * <li>{@link #getUncaughtExceptionHandler()}</li> <li>{@link #getContextClassLoader()}</li>
 * </ul><p> Thread state that will NEVER be restored includes: </p><ul> <li>Program counter, call
 * stack, and local variables. The thread will restart.</li> <li>Suspended status (see {@link
 * Thread#suspend()})</li> <li>{@link #getState()} == {@link State#TIMED_WAITING}</li> <li>{@link
 * #getState()} == {@link State#WAITING}</li> <li>{@link #getState()} == {@link State#BLOCKED}</li>
 * <li>{@link #getId()}</li> <li>{@link #holdsLock(Object)}</li> </ul>
 *
 * @author Chris Hennick
 */
@SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
public class LooperThread extends Thread implements Serializable, Cloneable {

  private static final LogPreFormatter LOG = new LogPreFormatter(LooperThread.class);
  private static final long serialVersionUID = -4387051967625864310L;
  /**
   * The preferred stack size for this thread, in bytes, if it was specified during construction; 0
   * otherwise. Held for serialization purposes.
   */
  protected final long stackSize;
  private final UncaughtExceptionHandlerWrapper uncaughtExceptionHandler =
      new UncaughtExceptionHandlerWrapper();
  /**
   * The thread holds this lock whenever it is being serialized or cloned or is running {@link
   * #iterate()} called by {@link #run()}.
   */
  protected transient Lock lock = new ReentrantLock();
  /**
   * The {@link ThreadGroup} this thread belongs to, if any. Held for serialization purposes.
   */
  protected @Nullable ThreadGroup group;
  /**
   * The {@link Runnable} that was passed into this thread's constructor, if any.
   */
  protected transient @Nullable Runnable target;
  /**
   * The name of this thread, if it has a non-default name. Held for serialization purposes.
   */
  protected @MonotonicNonNull String name = null;
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  private transient boolean alreadyTerminatedWhenDeserialized = false;
  private boolean interrupted = false;
  private boolean daemon = false;
  private int priority = Thread.NORM_PRIORITY;
  private State state = State.NEW;
  private @Nullable ClassLoader contextClassLoader = null;
  private @Nullable Runnable serialTarget;

  /**
   * Constructs a LooperThread with all properties as defaults. Protected because it does not set a
   * target, and thus should only be used in subclasses that override {@link #iterate()}.
   */
  protected LooperThread() {
    stackSize = 0;
  }

  /**
   * Constructs a LooperThread with the given target. {@code target} should only be null if called
   * from a subclass that overrides {@link #iterate()}.
   *
   * @param target If not null, the target this thread will run in {@link #iterate()}.
   */
  @SuppressWarnings("argument.type.incompatible")
  @EntryPoint
  public LooperThread(final @Nullable Runnable target) {
    super(target);
    this.target = target;
    stackSize = 0;
  }

  /**
   * Constructs a LooperThread that belongs to the given {@link ThreadGroup} and has the given
   * target. {@code target} should only be null if called from a subclass that overrides {@link
   * #iterate()}.
   *
   * @param group The ThreadGroup this thread will belong to.
   * @param target If not null, the target this thread will run in {@link #iterate()}.
   */
  @SuppressWarnings("argument.type.incompatible")
  @EntryPoint
  public LooperThread(final ThreadGroup group, final @Nullable Runnable target) {
    super(group, target);
    this.target = target;
    stackSize = 0;
  }

  /**
   * Constructs a LooperThread with the given name. Protected because it does not set a target, and
   * thus should only be used in subclasses that override {@link #iterate()}.
   *
   * @param name the thread name
   */
  @EntryPoint
  protected LooperThread(final String name) {
    super(name);
    stackSize = 0;
  }

  /**
   * Constructs a LooperThread with the given name and belonging to the given {@link ThreadGroup}.
   * Protected because it does not set a target, and thus should only be used in subclasses that
   * override {@link #iterate()}.
   *
   * @param group The ThreadGroup this thread will belong to.
   * @param name the thread name
   */
  @EntryPoint
  protected LooperThread(final ThreadGroup group, final String name) {
    super(group, name);
    setGroup(group);
    stackSize = 0;
  }

  /**
   * Constructs a LooperThread with the given name and target. {@code target} should only be null if
   * called from a subclass that overrides {@link #iterate()}.
   *
   * @param name the thread name
   * @param target If not null, the target this thread will run in {@link #iterate()}.
   */
  @SuppressWarnings("argument.type.incompatible")
  @EntryPoint
  public LooperThread(final @Nullable Runnable target, final String name) {
    super(target, name);
    this.target = target;
    stackSize = 0;
  }

  /**
   * Constructs a LooperThread with the given name and target, belonging to the given {@link
   * ThreadGroup}. {@code target} should only be null if called from a subclass that overrides
   * {@link #iterate()}.
   *
   * @param group The ThreadGroup this thread will belong to.
   * @param target If not null, the target this thread will run in {@link #iterate()}.
   * @param name the thread name
   */
  @SuppressWarnings("argument.type.incompatible")
  @EntryPoint
  public LooperThread(final ThreadGroup group, final @Nullable Runnable target, final String name) {
    super(group, target, name);
    this.target = target;
    setGroup(group);
    stackSize = 0;
  }

  /**
   * Constructs a LooperThread with the given name and target, belonging to the given {@link
   * ThreadGroup} and having the given preferred stack size. {@code target} should only be null if
   * called from a subclass that overrides {@link #iterate()}. See {@link Thread#Thread(ThreadGroup,
   * Runnable, String, long)} for caveats about specifying the stack size.
   *
   * @param group The ThreadGroup this thread will belong to.
   * @param target If not null, the target this thread will run in {@link #iterate()}.
   * @param name the thread name
   * @param stackSize the desired stack size for the new thread, or zero to indicate that this
   *     parameter is to be ignored.
   */
  @SuppressWarnings("argument.type.incompatible")
  public LooperThread(final ThreadGroup group, final @Nullable Runnable target, final String name,
      final long stackSize) {
    super(group, target, name, stackSize);
    this.target = target;
    setGroup(group);
    this.stackSize = stackSize;
  }

  private static @Nullable <T> T serializableOrNull(final @Nullable T object) {
    if (!(object instanceof Serializable)) {
      return null;
    }
    return object;
  }

  private void setGroup(@UnderInitialization LooperThread this,
      final @Nullable ThreadGroup group) {
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
   * Use readResolve rather than readObject, because stack size and thread group can only be
   * restored in Thread constructors.
   *
   * @return A LooperThread that will replace this one during deserialization.
   * @throws InvalidObjectException if this LooperThread's serial form is invalid.
   */
  protected Object readResolve()
      throws InvalidObjectException {
    target = serialTarget;
    if (name == null) {
      name = getName();
    }
    if (target == null) {
      target = new DummyTarget();
    }
    if (group == null) {
      group = castNonNull(Thread.currentThread().getThreadGroup());
    }
    final LooperThread t = readResolveConstructorWrapper();
    t.setDaemon(daemon);
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
        t.setStopped();
        t.alreadyTerminatedWhenDeserialized = true;
    }
    if (interrupted) {
      t.interrupt();
    }
    return t;
  }

  /**
   * @return A new LooperThread whose group is {@link #group}, whose target is {@link #target},
   *     whose name is {@link #name}, whose stack size is {@link #stackSize} and that either has its
   *     subclass fields copied from this one or overrides {@link #readResolve} to populate them.
   * @throws InvalidObjectException if this LooperThread's serial form is invalid.
   */
  @RequiresNonNull({"group", "name"})
  protected LooperThread readResolveConstructorWrapper()
      throws InvalidObjectException {
    return new LooperThread(group, target, name, stackSize);
  }

  private void setStopped() {
    if (getState() == State.NEW) {
      start();
    }
    interrupt();
    interrupted(); // Clear interrupted flag
  }

  /**
   * The task that will be iterated until it returns false. Cannot be abstract for serialization
   * reasons, but must be overridden in subclasses if they are instantiated without a target {@link
   * Runnable}.
   *
   * @return true if this thread should continue to iterate.
   * @throws InterruptedException if any.
   * @throws UnsupportedOperationException if this method has not been overridden.
   */
  protected boolean iterate() throws InterruptedException {
    if ((target == null) || (target instanceof DummyTarget)) {
      throw new UnsupportedOperationException("This method should be overridden, or else this "
          + "thread should have been created with a Serializable target!");
    } else {
      target.run();
      return true;
    }
  }

  @Override
  public final void run() {
    while (true) {
      try {
        lock.lockInterruptibly();
        try {
          if (!iterate()) {
            break;
          }
        } finally {
          lock.unlock();
        }
      } catch (final InterruptedException ignored) {
        interrupt();
        break;
      }
    }
  }

  @Override
  @SuppressWarnings("override.return.invalid")
  public @Nullable UncaughtExceptionHandler getUncaughtExceptionHandler() {
    return uncaughtExceptionHandler.wrapped;
  }

  @Override
  public void setUncaughtExceptionHandler(final UncaughtExceptionHandler handler) {
    uncaughtExceptionHandler.wrapped = handler;
  }

  @Override
  public State getState() {
    return alreadyTerminatedWhenDeserialized ? State.TERMINATED : super.getState();
  }

  @Override
  public synchronized void start() {
    if (alreadyTerminatedWhenDeserialized) {
      throw new IllegalThreadStateException(
          "This thread was deserialized from one that had already terminated");
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
      serialTarget = serializableOrNull(target);
      out.defaultWriteObject();
    } finally {
      lock.unlock();
    }
  }

  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @Override
  public LooperThread clone() {
    return CloneViaSerialization.clone(this);
  }

  private static class UncaughtExceptionHandlerWrapper
      implements UncaughtExceptionHandler, Serializable {

    private static final long serialVersionUID = -132520277274461153L;
    private @Nullable UncaughtExceptionHandler wrapped = null;

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
      LOG.error("Uncaught exception: %s", e);
      if (wrapped != null) {
        wrapped.uncaughtException(t, e);
      } else {
        final UncaughtExceptionHandler defaultHandler = getDefaultUncaughtExceptionHandler();
        if (defaultHandler == null) {
          e.printStackTrace();
          t.interrupt(); // necessary so that join() (which is final) will return
        } else {
          defaultHandler.uncaughtException(t, e);
        }
      }
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
      wrapped = serializableOrNull(wrapped);
      out.defaultWriteObject();
    }
  }

  private static class DummyTarget implements @Nullable Runnable {

    @Override
    public void run() {
      throw new UnsupportedOperationException("Dummy target");
    }
  }
}
