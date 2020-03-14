package io.github.pr0methean.betterrandom.seed;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A seed generator that wraps another, maintaining a buffer of previously-fetched bytes to
 * reduce the number of I/O calls. The buffer is only used when the requested seed is strictly
 * smaller than the buffer.
 */
public class BufferedSeedGenerator implements SeedGenerator {
  private static final long serialVersionUID = -2100305696539110970L;

  private final SeedGenerator delegate;
  /**
   * This could be replaced with a ReentrantReadWriteLock, with buffer consumption using the read
   * lock and refilling using the write lock. However, pos would then have to become an
   * AtomicInteger, so it would be worth the overhead only in rare corner cases with a large number
   * of threads. Furthermore, there would exist a slow path in which the consumption took place
   * under the write lock (when another thread had refilled the buffer while we'd been waiting for
   * the write lock, after initially finding the buffer empty under the read lock); and this slow
   * path would become more frequent as the number of concurrent threads increased, unless the
   * buffer size increased proportionately. Most of these corner cases are best handled by a
   * {@link RandomSeeder}.
   */
  private final Lock lock = new ReentrantLock(true);
  private final int size;
  private transient byte[] buffer;
  private transient volatile int pos;

  /**
   * Creates an instance.
   *
   * @param delegate the SeedGenerator to wrap
   * @param size the buffer size in bytes
   */
  public BufferedSeedGenerator(SeedGenerator delegate, int size) {
    this.delegate = delegate;
    this.size = size;
    initTransientFields();
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initTransientFields();
  }

  private void initTransientFields() {
    buffer = new byte[size];
    pos = size;
  }

  @Override public void generateSeed(byte[] output) throws SeedException {
    if (output.length >= size) {
      delegate.generateSeed(output);
      return;
    }
    lock.lock();
    try {
      int curPos = pos;
      int available = size - curPos;
      if (available >= output.length) {
        System.arraycopy(buffer, curPos, output, 0, output.length);
        curPos += output.length;
      } else {
        System.arraycopy(buffer, curPos, output, 0, available);
        delegate.generateSeed(buffer);
        curPos = output.length - available;
        System.arraycopy(buffer, 0, output, available, curPos);
      }
      pos = curPos;
    } finally {
      lock.unlock();
    }
  }

  @Override public boolean isWorthTrying() {
    return pos < size || delegate.isWorthTrying();
  }

  @Override public String toString() {
    return String.format("BufferedSeedGenerator(%s,%d)", delegate, size);
  }
}
