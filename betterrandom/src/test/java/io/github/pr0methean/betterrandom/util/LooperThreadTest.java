package io.github.pr0methean.betterrandom.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import io.github.pr0methean.betterrandom.MockException;
import io.github.pr0methean.betterrandom.TestingDeficiency;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.lang.Thread.State;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class LooperThreadTest {

  private static final String THREAD_NAME = "LooperThread for serialization test";
  private static final String GROUP_NAME = SerializableThreadGroup.class.getSimpleName();
  private static final long STACK_SIZE = 1_234_567;
  private static final Field THREAD_STACK_SIZE;
  private static final Field THREAD_TARGET;
  private static final AtomicLong iterationsRun = new AtomicLong();
  private static final AtomicBoolean shouldThrow = new AtomicBoolean(false);
  private static final AtomicBoolean exceptionHandlerRun = new AtomicBoolean(false);
  private static final Runnable TARGET = (Serializable & Runnable) () -> {
    if (shouldThrow.get()) {
      throw new MockException();
    }
    iterationsRun.addAndGet(1);
  };

  static {
    try {
      THREAD_STACK_SIZE = Thread.class.getDeclaredField("stackSize");
      THREAD_STACK_SIZE.setAccessible(true);
      THREAD_TARGET = Thread.class.getDeclaredField("target");
      THREAD_TARGET.setAccessible(true);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeTest
  public void setUp() {
    iterationsRun.set(0);
    shouldThrow.set(false);
    exceptionHandlerRun.set(false);
  }

  @SuppressWarnings("CallToThreadRun")
  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testMustOverrideIterate() {
    new LooperThread().run();
  }

  @Test
  public void testSerializable_notStarted() {
    LooperThread thread = new SkeletonLooperThread();
    LooperThread copy = CloneViaSerialization.clone(thread);
    assertNotSame(thread, copy);
    assertEquals(State.NEW, copy.getState());
  }

  @Test
  public void testSerializable_alreadyExited() {
    LooperThread thread = new SkeletonLooperThread();
    thread.start();
    try {
      thread.join();
    } catch (InterruptedException expected) {
    }
    LooperThread copy = CloneViaSerialization.clone(thread);
    assertNotSame(thread, copy);
    assertEquals(State.TERMINATED, copy.getState());
    try {
      copy.start();
      fail("Shouldn't be able to start a thread that's already terminated");
    } catch (final IllegalThreadStateException expected) {
    }
  }

  @SuppressWarnings("argument.type.incompatible")
  @Test
  public void testSerializable_nonSerializableState()
      throws InterruptedException, MalformedURLException, IllegalAccessException {
    LooperThread thread = new SkeletonLooperThread(() -> {
    });
    thread.setContextClassLoader(new MockClassLoader());
    thread.setUncaughtExceptionHandler((thread_, throwable) -> exceptionHandlerRun.set(true));
    LooperThread copy = CloneViaSerialization.clone(thread);
    assertNotSame(copy, thread);
    assertSame(copy.getContextClassLoader(), Thread.currentThread().getContextClassLoader());
    shouldThrow.set(true);
    copy.start();
    copy.join();
    assertFalse(exceptionHandlerRun.get());
  }

  @SuppressWarnings("dereference.of.nullable")
  @Test
  public void testSerializable_serializableState()
      throws InterruptedException, IllegalAccessException {
    LooperThread thread = new LooperThread(new SerializableThreadGroup(), TARGET, THREAD_NAME,
        STACK_SIZE);
    thread.setContextClassLoader(new SerializableClassLoader());
    thread.setUncaughtExceptionHandler(new SerializableUncaughtExceptionHandler());
    thread.setPriority(2);
    thread.setDaemon(true);
    LooperThread copy = CloneViaSerialization.clone(thread);
    assertNotSame(copy, thread);
    assertTrue(copy.getContextClassLoader() instanceof SerializableClassLoader);
    assertTrue(copy.getUncaughtExceptionHandler() instanceof SerializableUncaughtExceptionHandler);
    assertEquals(2, copy.getPriority());
    assertTrue(copy.isDaemon());
    assertEquals(THREAD_NAME, copy.getName());
    assertTrue(copy.getThreadGroup() instanceof SerializableThreadGroup);
    assertEquals(GROUP_NAME, copy.getThreadGroup().getName());
    assertEquals(STACK_SIZE, THREAD_STACK_SIZE.get(copy));
    shouldThrow.set(true);
    copy.start();
    copy.join();
    assertTrue(exceptionHandlerRun.get());
  }

  @TestingDeficiency // FIXME: Failing
  @Test(enabled = false)
  public void testDefaultUncaughtExceptionHandler() throws InterruptedException {
    final AtomicBoolean defaultHandlerCalled = new AtomicBoolean(false);
    UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
    try {
      Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
        defaultHandlerCalled.set(true);
      });
      FailingLooperThread failingThread = new FailingLooperThread();
      failingThread.start();
      failingThread.join();
      assertTrue(defaultHandlerCalled.get());
    } finally {
      Thread.setDefaultUncaughtExceptionHandler(oldHandler);
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

    public SerializableThreadGroup() {
      super();
    }
  }

  private static class SkeletonLooperThread extends LooperThread {

    public SkeletonLooperThread() {
    }

    public SkeletonLooperThread(Runnable target) {
      super(target);
    }

    public SkeletonLooperThread(ThreadGroup group, Runnable target, String name, long stackSize) {
      super(group, target, name, stackSize);
    }

    @RequiresNonNull({"group", "target", "name"})
    @Override
    protected LooperThread readResolveConstructorWrapper() throws InvalidObjectException {
      return new SkeletonLooperThread(group, target, name, stackSize);
    }

    @Override
    public boolean iterate() throws InterruptedException {
      TARGET.run();
      return iterationsRun.get() < 100;
    }
  }

  private static class FailingLooperThread extends LooperThread {
    public FailingLooperThread() {
      super("FailingLooperThread");
    }
    @Override
    public boolean iterate() {
      throw new MockException();
    }
  }

  private static class SerializableUncaughtExceptionHandler implements UncaughtExceptionHandler,
      Serializable {

    private static final long serialVersionUID = -4761296548510628117L;

    @Override
    public void uncaughtException(Thread t, Throwable e) {
      exceptionHandlerRun.set(true);
    }
  }

  private static class MockClassLoader extends ClassLoader {

  }

  private static class SerializableClassLoader extends ClassLoader implements Serializable {

  }
}
