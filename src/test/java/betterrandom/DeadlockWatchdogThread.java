package betterrandom;

import betterrandom.util.LogPreFormatter;
import betterrandom.util.LooperThread;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.logging.Logger;

public class DeadlockWatchdogThread extends LooperThread {
  private static final LogPreFormatter LOG = new LogPreFormatter(
      Logger.getLogger(DeadlockWatchdogThread.class.getName()));

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
    if (!deadlockFound) {
      long[] deadlockedThreadIds = THREAD_MX_BEAN.findDeadlockedThreads();
      for (long id : deadlockedThreadIds) {
        ThreadInfo threadInfo = THREAD_MX_BEAN.getThreadInfo(id, MAX_STACK_DEPTH);
        LOG.error("Deadlocked thread: %s",
            threadInfo.getThreadName());
        StackTraceElement[] stackTrace = threadInfo.getStackTrace();
        for (StackTraceElement element : stackTrace) {
          LOG.error("  " + element);
        }
      }
    }
  }
}
