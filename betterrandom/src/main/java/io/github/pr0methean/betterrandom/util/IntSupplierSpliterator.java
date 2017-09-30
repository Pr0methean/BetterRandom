package io.github.pr0methean.betterrandom.util;

import java.util.Spliterator.OfInt;
import java.util.Spliterator.OfLong;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/**
 * An unordered, concurrent {@link OfInt} that invokes an {@link IntSupplier} to get
 * its values and has a preset size.
 */
public class IntSupplierSpliterator implements OfInt {
  private final AtomicLong remaining;
  private final IntSupplier supplier;

  public IntSupplierSpliterator(final long size, final IntSupplier supplier) {
    this(new AtomicLong(size), supplier);
  }

  /** Used to share the AtomicLong between partitions. */
  private IntSupplierSpliterator(final AtomicLong remaining, final IntSupplier supplier) {
    this.remaining = remaining;
    this.supplier = supplier;
  }

  @Override
  public OfInt trySplit() {
    if (remaining.get() <= 0) {
      return null;
    } else {
      return new IntSupplierSpliterator(remaining, supplier);
    }
  }

  @Override
  public long estimateSize() {
    return remaining.get();
  }

  @Override
  public int characteristics() {
    return SIZED | CONCURRENT | IMMUTABLE | NONNULL;
  }

  @Override
  public boolean tryAdvance(final IntConsumer action) {
    if (remaining.decrementAndGet() >= 0) {
      action.accept(supplier.getAsInt());
      return true;
    } else {
      return false;
    }
  }
}
