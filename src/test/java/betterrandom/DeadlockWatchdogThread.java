package betterrandom;

import betterrandom.util.LogPreFormatter;
import betterrandom.util.LooperThread;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeadlockWatchdogThread extends LooperThread {
  private static final LogPreFormatter LOG = new LogPreFormatter(DeadlockWatchdogThread.class);

  private static final int MAX_STACK_DEPTH = 20;

  public static final DeadlockWatchdogThread INSTANCE = new DeadlockWatchdogThread();
  public static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

  static {
    INSTANCE.setDaemon(true);
  }

  private DeadlockWatchdogThread() {}

  public static void ensureStarted() {
    if (INSTANCE.getState() == State.NEW) {
      INSTANCE.start();
    }
  }
  
  private boolean deadlockFound = false;

  @Override
  public void iterate() throws InterruptedException {
    sleep(30_000);
    long[] threadsOfInterest;
    Level logLevel;
    threadsOfInterest = THREAD_MX_BEAN.findDeadlockedThreads();
    if (threadsOfInterest.length > 0) {
      LOG.error("DEADLOCKED THREADS FOUND");
      logLevel = Level.SEVERE;
    } else {
      logLevel = Level.INFO;
      threadsOfInterest = THREAD_MX_BEAN.getAllThreadIds();
    }
    for (long id : threadsOfInterest) {
      ThreadInfo threadInfo = THREAD_MX_BEAN.getThreadInfo(id, MAX_STACK_DEPTH);
      LOG.format(logLevel, threadInfo.getThreadName());
      StackTraceElement[] stackTrace = threadInfo.getStackTrace();
      for (StackTraceElement element : stackTrace) {
        LOG.format(logLevel, "  " + element);
      }
    }
  }
}
