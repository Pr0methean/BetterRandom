package io.github.pr0methean.betterrandom;

import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import io.github.pr0methean.betterrandom.util.LooperThread;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.logging.Level;

@SuppressWarnings("unused") // intermittently needed for debugging
public class DeadlockWatchdogThread extends LooperThread {

  private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
  private static final LogPreFormatter LOG = new LogPreFormatter(DeadlockWatchdogThread.class);
  private static final int MAX_STACK_DEPTH = 20;
  private static final int DEADLOCK_STATUS = 0xDEAD10CC;
  private static DeadlockWatchdogThread INSTANCE = new DeadlockWatchdogThread();

  private DeadlockWatchdogThread() {
    super("DeadlockWatchdogThread");
  }

  public static void ensureStarted() {
    synchronized (DeadlockWatchdogThread.class) {
      if (INSTANCE.getState() == State.TERMINATED) {
        INSTANCE = new DeadlockWatchdogThread();
      }
      if (INSTANCE.getState() == State.NEW) {
        INSTANCE.setDaemon(true);
        INSTANCE.setPriority(Thread.MAX_PRIORITY);
        INSTANCE.start();
      }
    }
  }

  public static void stopInstance() {
    synchronized (DeadlockWatchdogThread.class) {
      INSTANCE.interrupt();
      INSTANCE = new DeadlockWatchdogThread();
    }
  }

  @SuppressWarnings({"CallToSystemExit", "ConstantConditions"}) @Override public boolean iterate()
      throws InterruptedException {
    sleep(60_000);
    boolean deadlockFound = false;
    final Level logLevel;
    long[] threadsOfInterest = THREAD_MX_BEAN.findDeadlockedThreads();
    if ((threadsOfInterest != null) && (threadsOfInterest.length > 0)) {
      LOG.error("DEADLOCKED THREADS FOUND");
      logLevel = Level.SEVERE;
      deadlockFound = true;
    } else {
      logLevel = Level.INFO;
      threadsOfInterest = THREAD_MX_BEAN.getAllThreadIds();
      if (threadsOfInterest.length <= 0) {
        LOG.error("ThreadMxBean didn't return any thread IDs");
        return false;
      }
    }
    for (final long id : threadsOfInterest) {
      final ThreadInfo threadInfo = THREAD_MX_BEAN.getThreadInfo(id, MAX_STACK_DEPTH);
      LOG.format(logLevel, 0, threadInfo.getThreadName());
      final StackTraceElement[] stackTrace = threadInfo.getStackTrace();
      LOG.logStackTrace(logLevel, stackTrace);
    }
    if (deadlockFound) {
      // Fail fast if current context allows
      System.exit(DEADLOCK_STATUS);
    }
    return !deadlockFound; // Terminate when a deadlock is found
  }
}
