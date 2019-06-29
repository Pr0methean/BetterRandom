package io.github.pr0methean.betterrandom.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import io.github.pr0methean.betterrandom.MockException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

@SuppressWarnings("ClassLoaderInstantiation")
public class LooperThreadTest {

  private static class TestLooperThread extends LooperThread {
    AtomicBoolean shouldThrow = new AtomicBoolean(false);
    AtomicLong iterations = new AtomicLong(0);

    @Override protected boolean iterate() {
      if (shouldThrow.get()) {
        throw new MockException();
      }
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
      iterations.incrementAndGet();
      return true;
    }
  }

  private static final AtomicBoolean exceptionHandlerRun = new AtomicBoolean(false);

  @BeforeTest public void setUp() {
    exceptionHandlerRun.set(false);
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
    Thread.sleep(1000);
    assertEquals(sleepingThread.finishedIterations.get(), 1);
  }

  @Test public void testResurrect() throws InterruptedException {
    final TestLooperThread testLooperThread = new TestLooperThread();
    try {
      testLooperThread.shouldThrow.set(true);
      testLooperThread.start();
      int waits = 100;
      while (testLooperThread.isRunning()) {
        waits--;
        assertTrue(waits >= 0, "Timed out waiting for test looper thread to die");
        Thread.sleep(10);
      }
      assertEquals(testLooperThread.getState(), Thread.State.TERMINATED);
      testLooperThread.shouldThrow.set(false);
      testLooperThread.start();
      assertTrue(testLooperThread.isRunning());
      waits = 100;
      while (testLooperThread.iterations.get() == 0) {
        waits--;
        assertTrue(waits >= 0, "Timed out waiting for test looper thread to resume running");
        Thread.sleep(10);
      }
    } finally {
      testLooperThread.interrupt();
    }
  }

  private static class FailingLooperThread extends LooperThread {

    private FailingLooperThread() {
      super("FailingLooperThread");
    }

    @Override public boolean iterate() {
      throw new MockException();
    }
  }

  private static class SleepingLooperThread extends LooperThread {
    protected final AtomicLong finishedIterations = new AtomicLong(0);
    private SleepingLooperThread() {
      super("SleepingLooperThread");
    }

    @Override public boolean iterate() throws InterruptedException {
      Thread.sleep(10);
      finishedIterations.incrementAndGet();
      return false;
    }
  }
}
