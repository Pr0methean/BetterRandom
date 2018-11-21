package io.github.pr0methean.betterrandom.util;

import com.google.common.collect.ImmutableMap;
import io.github.pr0methean.betterrandom.MockException;
import io.github.pr0methean.betterrandom.TestUtils;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java8.util.function.Consumer;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

@SuppressWarnings("ClassLoaderInstantiation")
public class LooperThreadTest {

  private static final long STACK_SIZE = 1_234_567;
  private static final AtomicBoolean shouldThrow = new AtomicBoolean(false);
  private static final AtomicBoolean exceptionHandlerRun = new AtomicBoolean(false);
  private static final Runnable TARGET = new Runnable() {
    @Override public void run() {
      if (shouldThrow.get()) {
        throw new MockException();
      } else {
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
  };

  @Test public void testConstructors() {
    TestUtils.testConstructors(LooperThread.class, false, ImmutableMap
            .of(ThreadGroup.class, new ThreadGroup("Test ThreadGroup"), Runnable.class, TARGET,
                String.class, "Test LooperThread", long.class, STACK_SIZE),
        new Consumer<LooperThread>() {
          @Override public void accept(LooperThread thread) {
            thread.start();
            try {
              assertTrue(thread.awaitIteration(1, TimeUnit.SECONDS));
            } catch (final InterruptedException e) {
              throw new AssertionError(e);
            }
          }
        });
  }

  @BeforeTest public void setUp() {
    shouldThrow.set(false);
    exceptionHandlerRun.set(false);
  }

  @SuppressWarnings("CallToThreadRun")
  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testMustOverrideIterate() {
    new LooperThread().run();
  }

  @Test public void testDefaultUncaughtExceptionHandler() throws InterruptedException {
    final AtomicBoolean defaultHandlerCalled = new AtomicBoolean(false);
    final UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
    try {
      Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
        @Override public void uncaughtException(Thread thread, Throwable throwable) {
          defaultHandlerCalled.set(true);
        }
      });
      final FailingLooperThread failingThread = new FailingLooperThread();
      failingThread.start();
      failingThread.join();
      Thread.sleep(1000);
      assertTrue(defaultHandlerCalled.get());
    } finally {
      Thread.setDefaultUncaughtExceptionHandler(oldHandler);
    }
  }

  @Test public void testAwaitIteration() throws InterruptedException {
    final SleepingLooperThread sleepingThread = new SleepingLooperThread();
    sleepingThread.start();
    try {
      assertTrue(sleepingThread.awaitIteration(3, TimeUnit.SECONDS));
      // Now do so again, to ensure the thread still runs after returning
      assertTrue(sleepingThread.awaitIteration(3, TimeUnit.SECONDS));
    } finally {
      sleepingThread.interrupt();
    }
  }

  private static class FailingLooperThread extends LooperThread {

    public FailingLooperThread() {
      super("FailingLooperThread");
    }

    @Override public boolean iterate() {
      throw new MockException();
    }
  }

  private static class SleepingLooperThread extends LooperThread {

    public SleepingLooperThread() {
      super("SleepingLooperThread");
    }

    @Override public boolean iterate() throws InterruptedException {
      sleep(100);
      return finishedIterations.get() < 50;
    }
  }
}
