package io.github.pr0methean.betterrandom.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import com.google.common.util.concurrent.Uninterruptibles;
import io.github.pr0methean.betterrandom.MockException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

// FIXME: Sleep gets interrupted for no apparent reason, so have to use sleepUninterruptibly
public class LooperTest {

  private static class TestLooper extends Looper {
    private static final long serialVersionUID = 4931153919188474618L;
    final AtomicBoolean shouldThrow = new AtomicBoolean(false);
    final AtomicLong iterations = new AtomicLong(0);

    @Override protected boolean iterate() {
      if (shouldThrow.get()) {
        throw new MockException();
      }
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        fail("Should not have been interrupted", e);
      }
      iterations.incrementAndGet();
      return true;
    }
  }

  private static final AtomicBoolean exceptionHandlerRun = new AtomicBoolean(false);

  @BeforeTest public void setUp() {
    exceptionHandlerRun.set(false);
  }

  @Test public void testDefaultUncaughtExceptionHandler() {
    final AtomicBoolean defaultHandlerCalled = new AtomicBoolean(false);
    final UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
    try {
      Thread.setDefaultUncaughtExceptionHandler(
          (thread, throwable) -> defaultHandlerCalled.set(true));
      final FailingLooper failingThread = new FailingLooper();
      failingThread.start();
      while (failingThread.isRunning()) {
        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
      }
      Uninterruptibles.sleepUninterruptibly(1000, TimeUnit.MILLISECONDS);
      assertTrue(defaultHandlerCalled.get());
    } finally {
      Thread.setDefaultUncaughtExceptionHandler(oldHandler);
    }
  }

  @Test public void testAwaitIteration() {
    final SleepingLooper sleepingThread = new SleepingLooper();
    sleepingThread.start();
    sleepingThread.startLatch.countDown();
    Uninterruptibles.sleepUninterruptibly(1000, TimeUnit.MILLISECONDS);
    assertEquals(sleepingThread.finishedIterations.get(), 1);
  }

  // FIXME: Spurious interrupts
  @Test public void testResurrect() {
    final TestLooper testLooperThread = new TestLooper();
    try {
      testLooperThread.shouldThrow.set(true);
      testLooperThread.start();
      int waits = 100;
      while (testLooperThread.isRunning()) {
        waits--;
        assertTrue(waits >= 0, "Timed out waiting for test looper thread to die");
        Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
      }
      assertFalse(testLooperThread.isRunning());
      testLooperThread.shouldThrow.set(false);
      testLooperThread.start();
      assertTrue(testLooperThread.isRunning());
      waits = 100;
      while (testLooperThread.iterations.get() == 0) {
        waits--;
        assertTrue(waits >= 0, "Timed out waiting for test looper thread to resume running");
        Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
      }
    } finally {
      testLooperThread.interrupt();
    }
  }

  private static class FailingLooper extends Looper {

    private static final long serialVersionUID = 9113510029311710617L;

    private FailingLooper() {
      super("FailingLooper");
    }

    @Override public boolean iterate() {
      throw new MockException();
    }
  }

  private static class SleepingLooper extends Looper {
    private static final long serialVersionUID = -3389447813822333325L;
    protected final AtomicLong finishedIterations = new AtomicLong(0);
    protected final CountDownLatch startLatch = new CountDownLatch(2);

    private SleepingLooper() {
      super("SleepingLooper");
    }

    @Override public boolean iterate() throws InterruptedException {
      startLatch.countDown();
      startLatch.await();
      Thread.sleep(10);
      finishedIterations.incrementAndGet();
      return false;
    }
  }
}
