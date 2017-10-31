package io.github.pr0methean.betterrandom.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import com.google.common.collect.ImmutableMap;
import io.github.pr0methean.betterrandom.DeadlockWatchdogThread;
import io.github.pr0methean.betterrandom.MockException;
import io.github.pr0methean.betterrandom.TestUtils;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.lang.Thread.State;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java8.util.function.Consumer;
import javax.annotation.Nullable;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

@SuppressWarnings("ClassLoaderInstantiation")
public class LooperThreadTest {

  private static final String THREAD_NAME = "LooperThread for serialization test";
  private static final String GROUP_NAME = SerializableThreadGroup.class.getSimpleName();
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
        .of(ThreadGroup.class, new SerializableThreadGroup(), Runnable.class, TARGET, String.class,
            "Test LooperThread", long.class, STACK_SIZE), new Consumer<SkeletonLooperThread>() {
      @Override public void accept(SkeletonLooperThread thread) {
        CloneViaSerialization.clone(thread).start();
      }
    });
  }

  @BeforeClass public void setUpClass() {
    DeadlockWatchdogThread.ensureStarted();
  }

  @AfterClass public void tearDownClass() {
    DeadlockWatchdogThread.stopInstance();
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

  @Test public void testSerializable_notStarted() {
    final LooperThread thread = new SkeletonLooperThread();
    final LooperThread copy = CloneViaSerialization.clone(thread);
    assertNotSame(copy, thread);
    assertEquals(copy.getState(), State.NEW);
  }

  @Test public void testSerializable_alreadyExited() {
    final LooperThread thread = new SkeletonLooperThread();
    thread.start();
    try {
      thread.join();
    } catch (final InterruptedException expected) {
    }
    final LooperThread copy = CloneViaSerialization.clone(thread);
    assertNotSame(copy, thread);
    assertEquals(copy.getState(), State.TERMINATED);
    try {
      copy.start();
      fail("Shouldn't be able to start a thread that's already terminated");
    } catch (final IllegalThreadStateException expected) {
    }
  }

  @SuppressWarnings("argument.type.incompatible") @Test
  public void testSerializable_nonSerializableState()
      throws InterruptedException, MalformedURLException, IllegalAccessException {
    final LooperThread thread = new SkeletonLooperThread(new Runnable() {
      @Override public void run() {
      }
    });
    thread.setContextClassLoader(new MockClassLoader());
    thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
      @Override public void uncaughtException(Thread thread_, Throwable throwable) {
        exceptionHandlerRun.set(true);
      }
    });
    final LooperThread copy = CloneViaSerialization.clone(thread);
    assertNotSame(copy, thread);
    assertSame(copy.getContextClassLoader(), Thread.currentThread().getContextClassLoader());
    shouldThrow.set(true);
    copy.start();
    copy.join();
    assertFalse(exceptionHandlerRun.get());
  }

  @SuppressWarnings("dereference.of.nullable") @Test
  public void testSerializable_serializableState()
      throws InterruptedException, IllegalAccessException {
    final LooperThread thread =
        new LooperThread(new SerializableThreadGroup(), TARGET, THREAD_NAME, STACK_SIZE);
    thread.setContextClassLoader(new SerializableClassLoader());
    thread.setUncaughtExceptionHandler(new SerializableUncaughtExceptionHandler());
    thread.setPriority(2);
    thread.setDaemon(true);
    final LooperThread copy = CloneViaSerialization.clone(thread);
    assertNotSame(copy, thread);
    assertTrue(copy.getContextClassLoader() instanceof SerializableClassLoader);
    assertTrue(copy.getUncaughtExceptionHandler() instanceof SerializableUncaughtExceptionHandler);
    assertEquals(copy.getPriority(), 2);
    assertTrue(copy.isDaemon());
    assertEquals(copy.getName(), THREAD_NAME);
    assertTrue(copy.getThreadGroup() instanceof SerializableThreadGroup);
    assertEquals(copy.getThreadGroup().getName(), GROUP_NAME);
    assertEquals(THREAD_STACK_SIZE.get(copy), STACK_SIZE);
    shouldThrow.set(true);
    copy.start();
    copy.join();
    assertTrue(exceptionHandlerRun.get());
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
      assertTrue(sleepingThread.awaitIteration());
    } finally {
      sleepingThread.interrupt();
    }
  }

  @Test public void testAwaitIterationTimeout() throws InterruptedException {
    SleepingLooperThread sleepingThread = new SleepingLooperThread();
    sleepingThread.start();
    try {
      assertTrue(sleepingThread.awaitIteration(5, TimeUnit.SECONDS));
    } finally {
      sleepingThread.interrupt();
    }
  }

  /**
   * Intermediate used to give {@link SerializableThreadGroup} a parameterless super constructor for
   * deserialization purposes.
   */
  private static class SerializableThreadGroupSurrogate extends ThreadGroup {

    public SerializableThreadGroupSurrogate() {
      super(GROUP_NAME);
    }
  }

  private static class SerializableThreadGroup extends SerializableThreadGroupSurrogate
      implements Serializable {

    private static final long serialVersionUID = 4660069266898564395L;

    public SerializableThreadGroup() {
    }
  }

  /** Must be public since ctors are accessed reflectively by {@link TestUtils} */
  public static class SkeletonLooperThread extends LooperThread {

    private static final long serialVersionUID = -6863326140536988360L;

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

    @Override protected LooperThread readResolveConstructorWrapper() throws InvalidObjectException {
      return new SkeletonLooperThread(serialGroup, target, name, stackSize);
    }

    @Override public boolean iterate() throws InterruptedException {
      TARGET.run();
      return finishedIterations.get() < 100;
    }
  }

  private static class FailingLooperThread extends LooperThread {

    private static final long serialVersionUID = -1882343225722025757L;

    public FailingLooperThread() {
      super("FailingLooperThread");
    }

    @Override public boolean iterate() {
      throw new MockException();
    }
  }

  private static class SleepingLooperThread extends LooperThread {

    private static final long serialVersionUID = -2726985092790511416L;

    public SleepingLooperThread() {
      super("SleepingLooperThread");
    }

    @Override public boolean iterate() throws InterruptedException {
      sleep(500);
      TARGET.run();
      return finishedIterations.get() < 25;
    }
  }

  private static class SerializableUncaughtExceptionHandler
      implements UncaughtExceptionHandler, Serializable {

    private static final long serialVersionUID = -4761296548510628117L;

    @Override public void uncaughtException(final Thread t, final Throwable e) {
      exceptionHandlerRun.set(true);
    }
  }

  @SuppressWarnings("CustomClassloader")
  private static class MockClassLoader extends ClassLoader {

  }

  @SuppressWarnings("CustomClassloader")
  private static class SerializableClassLoader extends ClassLoader implements Serializable {

    private static final long serialVersionUID = -5540517522704769624L;
  }
}
