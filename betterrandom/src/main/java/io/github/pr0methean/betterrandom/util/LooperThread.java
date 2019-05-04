package io.github.pr0methean.betterrandom.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;

/**
 * Thread that loops a given task until interrupted (or until JVM shutdown, if it {@link
 * #isDaemon() is a daemon thread}), with the iterations being transactional.
 */
public abstract class LooperThread implements Runnable {

  protected final AtomicLong finishedIterations = new AtomicLong(0);
  /**
   * The thread holds this lock whenever it is being serialized or cloned or is running {@link
   * #iterate()}.
   */
  protected final Lock lock = new ReentrantLock(true);
  protected final Condition endOfIteration = lock.newCondition();
  protected final Thread thread;
  /**
   * The {@link Runnable} that was passed into this thread's constructor, if any.
   */
  @Nullable protected Runnable target;

  /**
   * Constructs a LooperThread with all properties as defaults. Protected because it does not set a
   * target, and thus should only be used in subclasses that override {@link #iterate()}.
   */
  protected LooperThread() {
    thread = new Thread();
  }

  /**
   * Constructs a LooperThread with a thread name.
   *
   * @deprecated Being replaced with a ThreadFactory parameter, so that threads can die and be
   * replaced.
   */
  @Deprecated
  protected LooperThread(String name) {
    thread = new Thread(name);
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
      while (!isInterrupted() && (getState() != Thread.State.TERMINATED) && (finishedIterations.get()
          == previousFinishedIterations)) {
        endOfIteration.await(time, unit);
      }
      return finishedIterations.get() != previousFinishedIterations;
    } finally {
      lock.unlock();
    }
  }

  public void start() {
    thread.start();
  }

  public void run() {
    thread.run();
  }

  public void interrupt() {
    thread.interrupt();
  }

  public boolean isInterrupted() {
    return thread.isInterrupted();
  }

  public void setPriority(int newPriority) {
    thread.setPriority(newPriority);
  }

  public int getPriority() {
    return thread.getPriority();
  }

  public void setName(String name) {
    thread.setName(name);
  }

  public String getName() {
    return thread.getName();
  }

  public ThreadGroup getThreadGroup() {
    return thread.getThreadGroup();
  }

  public int countStackFrames() {
    return thread.countStackFrames();
  }

  public void join(long millis) throws InterruptedException {
    thread.join(millis);
  }

  public void join(long millis, int nanos) throws InterruptedException {
    thread.join(millis, nanos);
  }

  public void join() throws InterruptedException {
    thread.join();
  }

  public void setDaemon(boolean on) {
    thread.setDaemon(on);
  }

  public boolean isDaemon() {
    return thread.isDaemon();
  }

  public void checkAccess() {
    thread.checkAccess();
  }

  public String toString() {
    return thread.toString();
  }

  public ClassLoader getContextClassLoader() {
    return thread.getContextClassLoader();
  }

  public void setContextClassLoader(ClassLoader cl) {
    thread.setContextClassLoader(cl);
  }

  public StackTraceElement[] getStackTrace() {
    return thread.getStackTrace();
  }

  public long getId() {
    return thread.getId();
  }

  public Thread.State getState() {
    return thread.getState();
  }

  public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
    return thread.getUncaughtExceptionHandler();
  }

  public void setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler eh) {
    thread.setUncaughtExceptionHandler(eh);
  }

  private class Runner implements Runnable {

    /**
     * Runs {@link #iterate()} until either it returns false or this thread is interrupted.
     */
    @Override public void run() {
      while (true) {
        try {
          lock.lockInterruptibly();
          try {
            final boolean shouldContinue = iterate();
            finishedIterations.getAndIncrement();
            if (!shouldContinue) {
              break;
            }
          } finally {
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
}
