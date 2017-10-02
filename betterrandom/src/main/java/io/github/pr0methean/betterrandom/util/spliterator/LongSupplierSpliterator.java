package io.github.pr0methean.betterrandom.util.spliterator;

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
  private final AtomicLong splitsRemaining;
  private final LongSupplier supplier;

  /**
   * Create an instance.
   *
   * @param size The maximum number of values to output.
   * @param supplier The supplier to wrap.
   */
  public LongSupplierSpliterator(final long size, final LongSupplier supplier) {
    this(new AtomicLong(size), new AtomicLong(Long.SIZE - Long.numberOfLeadingZeros(size)),
        supplier);
  }

  /** Used to share the AtomicLong between partitions. */
  private LongSupplierSpliterator(final AtomicLong remaining, final AtomicLong splitsRemaining,
      final LongSupplier supplier) {
    this.remaining = remaining;
    this.splitsRemaining = splitsRemaining;
    this.supplier = supplier;
  }

  @SuppressWarnings("override.return.invalid") // actually is nullable in the interface
  @Override
  public @Nullable OfLong trySplit() {
    return ((splitsRemaining.getAndDecrement() <= 0) || (remaining.get() <= 0))
        ? null
        : new LongSupplierSpliterator(remaining, splitsRemaining, supplier);
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
