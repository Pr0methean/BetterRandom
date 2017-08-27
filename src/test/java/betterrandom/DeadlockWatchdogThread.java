package betterrandom;

import betterrandom.util.LogPreFormatter;
import betterrandom.util.LooperThread;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.logging.Logger;

public class DeadlockWatchdogThread extends LooperThread {
  private static final LogPreFormatter LOG = new LogPreFormatter(
      Logger.getLogger(DeadlockWatchdogThread.class.getName()));

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

  @Override
  public void iterate() throws InterruptedException {
    sleep(30_000);
    long[] deadlockedThreadIds = THREAD_MX_BEAN.findDeadlockedThreads();
    for (long id : deadlockedThreadIds) {
      ThreadInfo threadInfo = THREAD_MX_BEAN.getThreadInfo(id);
      StringBuilder stackTraceBuilder = new StringBuilder();
      for (StackTraceElement element : threadInfo.getStackTrace()) {
        stackTraceBuilder.append(element);
        stackTraceBuilder.append('\n');
      }
      LOG.error("\nDeadlocked thread: %s\n\n%s",
          threadInfo.getThreadName(), stackTraceBuilder);
    }
  }
}
