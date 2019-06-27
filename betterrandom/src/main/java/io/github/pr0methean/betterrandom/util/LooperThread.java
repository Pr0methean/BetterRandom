package io.github.pr0methean.betterrandom.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wraps a thread that loops a given task until interrupted, with the iterations being
 * transactional.
 */
public abstract class LooperThread implements Serializable {

  protected final AtomicLong finishedIterations = new AtomicLong(0);
  /**
   * The thread holds this lock whenever it is running {@link #iterate()}.
   */
  protected final Lock lock = new ReentrantLock(true);
  protected final Lock threadLock = new ReentrantLock();
  protected final Condition endOfIteration = lock.newCondition();
  protected transient volatile Thread thread;
  protected final ThreadFactory factory;
  private volatile boolean running; // must be tracked for deserialization

  /**
   * Constructs a LooperThread with all properties as defaults.
   */
  protected LooperThread() {
    this(Thread::new);
  }

  /**
   * Constructs a LooperThread with a thread name.
   *
   * @deprecated Being replaced with a ThreadFactory parameter, so that threads can die and be
   * replaced.
   */
  @Deprecated
  protected LooperThread(String name) {
    this(runnable -> new Thread(runnable, name));
  }

  protected LooperThread(ThreadFactory factory) {
    this.factory = factory;
    start();
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
   * @return true if this thread should iterate again.
   * @throws InterruptedException if interrupted in mid-execution.
   * @throws UnsupportedOperationException if this method has not been overridden and {@link
   *     #target} was not set to non-null during construction.
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted") protected abstract boolean iterate()
      throws InterruptedException;

  /**
   * Wait for the next iteration to finish, with a timeout. May wait longer in the event of a
   * spurious wakeup.
   * @param time the maximum time to wait
   * @param unit the time unit of the {@code time} argument
   * @return {@code false}  the waiting time detectably elapsed before an iteration finished, else
   *     {@code true}
   * @throws InterruptedException if thrown by {@link Condition#await(long, TimeUnit)}
   */
  public boolean awaitIteration(final long time, final TimeUnit unit) throws InterruptedException {
    final long previousFinishedIterations = finishedIterations.get();
    lock.lock();
    try {
      while (!thread.isInterrupted()
          && (getState() != Thread.State.TERMINATED) && (finishedIterations.get() == previousFinishedIterations)) {
        endOfIteration.await(time, unit);
      }
      return finishedIterations.get() != previousFinishedIterations;
    } finally {
      lock.unlock();
    }
  }

  protected void start() {
    threadLock.lock();
    try {
      if (thread == null || !thread.isAlive()) {
        thread = factory.newThread(this::run);
        thread.start();
      }
    } finally {
      threadLock.unlock();
    }
    running = true;
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

  /** Only used for testing. */
  @Deprecated
  void join() throws InterruptedException {
    thread.join();
  }

  @Override
  public String toString() {
    return thread.toString();
  }

  protected Thread.State getState() {
    threadLock.lock();
    try {
      return thread.getState();
    } finally {
      threadLock.unlock();
    }
  }

  /**
   * Runs {@link #iterate()} until either it returns false or this thread is interrupted.
   */
  private void run() {
    while (true) {
      try {
        lock.lockInterruptibly();
        try {
          if (!iterate()) {
            interrupt();
            break;
          }
        } finally {
          finishedIterations.getAndIncrement();
          endOfIteration.signalAll();
          lock.unlock();
        }
      } catch (final InterruptedException ignored) {
        interrupt();
        break;
      }
    }
  }
}
