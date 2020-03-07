package io.github.pr0methean.betterrandom;

import io.github.pr0methean.betterrandom.util.Looper;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused") // intermittently needed for debugging
public class DeadlockWatchdog extends Looper {

  private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
  private static final Logger LOG = LoggerFactory.getLogger(DeadlockWatchdog.class);
  private static final int MAX_STACK_DEPTH = 20;
  private static final int DEADLOCK_STATUS = 0xDEAD10CC;
  public static final int POLL_INTERVAL = 5_000;
  private static final long serialVersionUID = 1160104387427407739L;
  private static DeadlockWatchdog INSTANCE = new DeadlockWatchdog();

  private DeadlockWatchdog() {
    super(runnable -> {
      Thread thread = new Thread(runnable);
      thread.setDaemon(true);
      thread.setPriority(Thread.MAX_PRIORITY);
      return thread;
    });
  }

  public static synchronized void ensureStarted() {
    INSTANCE.start();
  }

  public static synchronized void stopInstance() {
    INSTANCE.interrupt();
    INSTANCE = new DeadlockWatchdog();
  }

  @SuppressWarnings({"CallToSystemExit", "ConstantConditions", "ObjectAllocationInLoop"}) @Override
  public boolean iterate() throws InterruptedException {
    Thread.sleep(POLL_INTERVAL);
    boolean deadlockFound = false;
    long[] threadsOfInterest = THREAD_MX_BEAN.findDeadlockedThreads();
    if ((threadsOfInterest != null) && (threadsOfInterest.length > 0)) {
      LOG.error("DEADLOCKED THREADS FOUND");
      deadlockFound = true;
    } else {
      threadsOfInterest = THREAD_MX_BEAN.getAllThreadIds();
      if (threadsOfInterest.length <= 0) {
        LOG.error("ThreadMxBean didn't return any thread IDs");
        return false;
      }
    }
    for (final long id : threadsOfInterest) {
      final ThreadInfo threadInfo = THREAD_MX_BEAN.getThreadInfo(id, MAX_STACK_DEPTH);
      final StackTraceElement[] stackTrace = threadInfo.getStackTrace();
      final Throwable t = new StackTraceHolder(threadInfo.getThreadName(), stackTrace);
      if (deadlockFound) {
        LOG.error("A deadlocked thread:", t);
      } else {
        LOG.info("A running thread:", t);
      }
    }
    if (deadlockFound) {
      // Fail fast if current context allows
      System.exit(DEADLOCK_STATUS);
    }
    return !deadlockFound; // Terminate when a deadlock is found
  }
}
