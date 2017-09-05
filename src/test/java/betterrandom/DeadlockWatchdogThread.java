package betterrandom;

import betterrandom.util.LogPreFormatter;
import betterrandom.util.LooperThread;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.logging.Level;

public class DeadlockWatchdogThread extends LooperThread {

  public static final DeadlockWatchdogThread INSTANCE = new DeadlockWatchdogThread();
  public static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
  private static final LogPreFormatter LOG = new LogPreFormatter(DeadlockWatchdogThread.class);
  private static final int MAX_STACK_DEPTH = 20;
  private static final long serialVersionUID = 9118178318042580320L;

  private DeadlockWatchdogThread() {
  }

  public static void ensureStarted() {
    if (INSTANCE.getState() == State.NEW) {
      INSTANCE.setDaemon(true);
      INSTANCE.setPriority(Thread.MAX_PRIORITY);
      INSTANCE.setName("DeadlockWatchdogThread");
      INSTANCE.start();
    }
  }

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
