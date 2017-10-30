package io.github.pr0methean.betterrandom;

import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import io.github.pr0methean.betterrandom.util.LooperThread;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.logging.Level;

public class DeadlockWatchdogThread extends LooperThread {

  private static DeadlockWatchdogThread INSTANCE = new DeadlockWatchdogThread();
  private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
  private static final LogPreFormatter LOG = new LogPreFormatter(DeadlockWatchdogThread.class);
  private static final int MAX_STACK_DEPTH = 20;
  private static final long serialVersionUID = 9118178318042580320L;

  private DeadlockWatchdogThread() {
    super("DeadlockWatchdogThread");
  }

  public static synchronized void ensureStarted() {
    if (INSTANCE.getState() == State.TERMINATED) {
      INSTANCE = new DeadlockWatchdogThread();
    }
    if (INSTANCE.getState() == State.NEW) {
      INSTANCE.setDaemon(true);
      INSTANCE.setPriority(Thread.MAX_PRIORITY);
      INSTANCE.start();
    }
  }

  public static synchronized void stopInstance() {
    INSTANCE.interrupt();
    INSTANCE = new DeadlockWatchdogThread();
  }

  @Override public boolean iterate() throws InterruptedException {
    boolean deadlockFound = false;
    Level logLevel;
    long[] threadsOfInterest = THREAD_MX_BEAN.findDeadlockedThreads();
    if (threadsOfInterest != null && threadsOfInterest.length > 0) {
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
    for (long id : threadsOfInterest) {
      ThreadInfo threadInfo = THREAD_MX_BEAN.getThreadInfo(id, MAX_STACK_DEPTH);
      LOG.format(logLevel, 0, threadInfo.getThreadName());
      StackTraceElement[] stackTrace = threadInfo.getStackTrace();
      LOG.logStackTrace(logLevel, stackTrace);
    }
    sleep(5_000);
    return !deadlockFound; // Terminate when a deadlock is found
  }
}
