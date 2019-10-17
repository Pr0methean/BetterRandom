package io.github.pr0methean.betterrandom.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wraps a thread that loops a given task until interrupted, with the iterations being
 * transactional.
 */
public abstract class LooperThread implements Serializable {

  /**
   * Singleton-ization of {@link Executors#defaultThreadFactory()}.
   */
  protected static final ThreadFactory DEFAULT_THREAD_FACTORY = Executors.defaultThreadFactory();
  private static final long serialVersionUID = -4790652062170305318L;

  /**
   * The thread holds this lock whenever it is running {@link #iterate()}.
   */
  protected final Lock lock = new ReentrantLock(true);
  protected final Lock threadLock = new ReentrantLock();
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  protected transient volatile Thread thread;
  protected final ThreadFactory factory;
  private volatile boolean running; // determines whether to start when deserialized
  private volatile boolean everStarted; // tracked for getState()

  /**
   * Constructs a LooperThread with all properties as defaults. The thread is not started in the
   * constructor, because subclass fields won't have been initialized.
   */
  protected LooperThread() {
    this(DEFAULT_THREAD_FACTORY);
  }

  /**
   * Constructs a LooperThread with a thread name. The thread is not started in the
   * constructor, because subclass fields won't have been initialized.
   *
   * @param name the name of the thread to create
   */
  protected LooperThread(final String name) {
    this(r -> {
      Thread thread = DEFAULT_THREAD_FACTORY.newThread(r);
      thread.setName(name);
      return thread;
    });
  }

  /**
   * Constructs a LooperThread with a given thread factory. The thread is not started in the
   * constructor, because subclass fields won't have been initialized.
   *
   * @param factory the thread factory that will create this instance's thread
   */
  protected LooperThread(ThreadFactory factory) {
    this.factory = factory;
  }

  public boolean isRunning() {
    threadLock.lock();
    try {
      return thread != null && thread.isAlive();
    } finally {
      threadLock.unlock();
    }
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    if (running) {
      start();
    }
  }

  /**
   * The task that will be iterated until it returns false. Cannot be abstract for serialization
   * reasons, but must be overridden in subclasses if they are instantiated without a target {@link
   * Runnable}.
   *
   * @return true if this thread should iterate again.
   * @throws InterruptedException if interrupted in mid-execution.
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted") protected abstract boolean iterate()
      throws InterruptedException;

  protected void start() {
    threadLock.lock();
    try {
      if (thread == null || !thread.isAlive()) {
        thread = factory.newThread(this::run);
        thread.start();
        everStarted = true;
        running = true;
      }
    } finally {
      threadLock.unlock();
    }
  }

  public void interrupt() {
    threadLock.lock();
    try {
      running = false;
      if (thread != null) {
        thread.interrupt();
        thread = null;
      }
    } finally {
      threadLock.unlock();
    }
  }

  @Override public String toString() {
    return thread.toString();
  }

  protected Thread.State getState() {
    threadLock.lock();
    try {
      if (thread == null) {
        return everStarted ? Thread.State.TERMINATED : Thread.State.NEW;
      }
      return thread.getState();
    } finally {
      threadLock.unlock();
    }
  }

  /**
   * Runs {@link #iterate()} until either it returns false or this thread is interrupted.
   */
  private void run() {
    try {
      while (true) {
        lock.lockInterruptibly();
        try {
          if (!iterate()) {
            interrupt();
            return;
          }
        } finally {
          lock.unlock();
        }
      }
    } catch (final InterruptedException ignored) {
      interrupt();
    }
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    lock.lock();
    try {
      out.defaultWriteObject();
    } finally {
      lock.unlock();
    }
  }
}
