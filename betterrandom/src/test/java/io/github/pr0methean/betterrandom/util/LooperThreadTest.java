package io.github.pr0methean.betterrandom.util;

import static org.checkerframework.checker.nullness.NullnessUtil.castNonNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import io.github.pr0methean.betterrandom.TestUtil;
import java.io.Serializable;
import java.lang.Thread.State;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.testng.Assert;
import org.testng.TestException;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class LooperThreadTest {

  private static class SkeletonLooperThread extends LooperThread {
    @Override
    public boolean iterate() throws InterruptedException {
      if (shouldThrow.get()) {
        throw new TestUtil.MockException();
      }
      return iterationsRun.addAndGet(1) < 100;
    }
  }

  private static class SerializableUncaughtExceptionHandler implements UncaughtExceptionHandler, Serializable {
    private static final long serialVersionUID = -4761296548510628117L;
    @Override
    public void uncaughtException(Thread t, Throwable e) {
      exceptionHandlerRun.set(true);
    }
  }

  private static class MockClassLoader extends ClassLoader {}
  private static class SerializableClassLoader extends ClassLoader implements Serializable {}

  private static final Lock TEST_LOCK = new ReentrantLock();
  private static final AtomicLong iterationsRun = new AtomicLong();
  private static final AtomicBoolean shouldThrow = new AtomicBoolean(false);
  private static final     AtomicBoolean exceptionHandlerRun = new AtomicBoolean(false);

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
    LooperThread copy = TestUtil.serializeAndDeserialize(thread);
    assertNotSame(thread, copy);
    assertEquals(State.NEW, copy.getState());
  }

  @Test
  public void testSerializable_alreadyExited() {
    LooperThread thread = new SkeletonLooperThread();
    thread.start();
    try {
      thread.join();
    } catch (InterruptedException expected) {}
    LooperThread copy = TestUtil.serializeAndDeserialize(thread);
    assertNotSame(thread, copy);
    assertEquals(State.TERMINATED, copy.getState());
  }

  @SuppressWarnings("argument.type.incompatible")
  @Test
  public void testSerializable_nonSerializableState()
      throws InterruptedException, MalformedURLException {
    LooperThread thread = new SkeletonLooperThread();
    thread.setContextClassLoader(new MockClassLoader());
    thread.setUncaughtExceptionHandler((thread_, throwable) -> exceptionHandlerRun.set(true));
    LooperThread copy = TestUtil.serializeAndDeserialize(thread);
    assertSame(copy.getContextClassLoader(), Thread.currentThread().getContextClassLoader());
    assertNull(copy.getUncaughtExceptionHandler());
    shouldThrow.set(true);
    copy.start();
    copy.join();
    assertFalse(exceptionHandlerRun.get());
  }

  @SuppressWarnings("dereference.of.nullable")
  @Test
  public void testSerializable_serializableState() throws InterruptedException {
    LooperThread thread = new SkeletonLooperThread();
    thread.setContextClassLoader(new SerializableClassLoader());
    thread.setUncaughtExceptionHandler(new SerializableUncaughtExceptionHandler());
    LooperThread copy = TestUtil.serializeAndDeserialize(thread);
    assertTrue(copy.getContextClassLoader() instanceof SerializableClassLoader);
    assertTrue(copy.getUncaughtExceptionHandler() instanceof SerializableUncaughtExceptionHandler);
    shouldThrow.set(true);
    copy.start();
    copy.join();
    assertTrue(exceptionHandlerRun.get());
  }
}
