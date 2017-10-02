package io.github.pr0methean.betterrandom.util;

import java.util.Spliterator.OfLong;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An unordered, concurrent {@link OfLong} that invokes a {@link LongSupplier} to get its values and
 * has a preset size.
 */
public class LongSupplierSpliterator implements OfLong {

  private final AtomicLong remaining;
  private final LongSupplier supplier;

  public LongSupplierSpliterator(final long size, final LongSupplier supplier) {
    this(new AtomicLong(size), supplier);
  }

  /** Used to share the AtomicLong between partitions. */
  private LongSupplierSpliterator(final AtomicLong remaining, final LongSupplier supplier) {
    this.remaining = remaining;
    this.supplier = supplier;
  }

  @SuppressWarnings("override.return.invalid") // actually is nullable in the interface
  @Override
  public @Nullable OfLong trySplit() {
    return (remaining.get() <= 0) ? null : new LongSupplierSpliterator(remaining, supplier);
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
  public boolean tryAdvance(final LongConsumer action) {
    if (remaining.decrementAndGet() >= 0) {
      action.accept(supplier.getAsLong());
      return true;
    } else {
      return false;
    }
  }
}
