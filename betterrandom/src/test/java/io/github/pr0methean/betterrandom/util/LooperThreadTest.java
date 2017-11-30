package io.github.pr0methean.betterrandom.util;

import static org.testng.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import io.github.pr0methean.betterrandom.MockException;
import io.github.pr0methean.betterrandom.TestUtils;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java8.util.function.Consumer;
import javax.annotation.Nullable;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

@SuppressWarnings("ClassLoaderInstantiation")
public class LooperThreadTest {

  private static final String THREAD_NAME = "LooperThread for test";
  private static final long STACK_SIZE = 1_234_567;
  private static final Field THREAD_STACK_SIZE;
  private static final Field THREAD_TARGET;
  private static final AtomicBoolean shouldThrow = new AtomicBoolean(false);
  private static final AtomicBoolean exceptionHandlerRun = new AtomicBoolean(false);
  private static final Runnable TARGET = new Runnable() {
    @Override public void run() {
      if (shouldThrow.get()) {
        throw new MockException();
      }
    }
  };

  static {
    try {
      THREAD_STACK_SIZE = Thread.class.getDeclaredField("stackSize");
      THREAD_STACK_SIZE.setAccessible(true);
      THREAD_TARGET = Thread.class.getDeclaredField("target");
      THREAD_TARGET.setAccessible(true);
    } catch (final NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  @Test public void testAllPublicConstructors()
      throws IllegalAccessException, InstantiationException, InvocationTargetException {
    // Test SkeletonLooperThread instead of LooperThread so that protected ctors in LooperThread are
    // also covered
    TestUtils.testAllPublicConstructors(SkeletonLooperThread.class, ImmutableMap
        .of(ThreadGroup.class, new ThreadGroup("Test ThreadGroup"), Runnable.class, TARGET, String.class,
            "Test LooperThread", long.class, STACK_SIZE),
new Consumer<SkeletonLooperThread>() {
      @Override public void accept(SkeletonLooperThread thread) {
        thread.start();
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
    SleepingLooperThread sleepingThread = new SleepingLooperThread();
    sleepingThread.start();
    try {
      assertTrue(sleepingThread.awaitIteration(5, TimeUnit.SECONDS));
    } finally {
      sleepingThread.interrupt();
    }
  }

  /** Must be public since ctors are accessed reflectively by {@link TestUtils} */
  public static class SkeletonLooperThread extends LooperThread {

    public SkeletonLooperThread() {
    }

    public SkeletonLooperThread(final Runnable target) {
      super(target);
    }

    public SkeletonLooperThread(final ThreadGroup group, @Nullable final Runnable target) {
      super(group, target);
    }

    public SkeletonLooperThread(final String name) {
      super(name);
    }

    public SkeletonLooperThread(final ThreadGroup group, final String name) {
      super(group, name);
    }

    public SkeletonLooperThread(@Nullable final Runnable target, final String name) {
      super(target, name);
    }

    public SkeletonLooperThread(final ThreadGroup group, @Nullable final Runnable target,
        final String name) {
      super(group, target, name);
    }

    public SkeletonLooperThread(final ThreadGroup group, final Runnable target, final String name,
        final long stackSize) {
      super(group, target, name, stackSize);
    }

    @Override public boolean iterate() throws InterruptedException {
      TARGET.run();
      return finishedIterations.get() < 100;
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
      sleep(500);
      TARGET.run();
      return finishedIterations.get() < 25;
    }
  }

  @SuppressWarnings("CustomClassloader")
  private static class MockClassLoader extends ClassLoader {

  }
}
