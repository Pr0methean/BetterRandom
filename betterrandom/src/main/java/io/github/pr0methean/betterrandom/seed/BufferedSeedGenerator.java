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
  private final Lock lock = new ReentrantLock();
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
    } else {
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
  }

  @Override public boolean isWorthTrying() {
    return pos < size || delegate.isWorthTrying();
  }

  @Override public String toString() {
    return String.format("BufferedSeedGenerator(%s,%d)", delegate, size);
  }
}
