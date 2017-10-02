package io.github.pr0methean.betterrandom.util.spliterator;

import java.util.Spliterator.OfInt;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An unordered, concurrent {@link OfInt} that invokes an {@link IntSupplier} to get its values and
 * has a preset size.
 */
public class IntSupplierSpliterator implements OfInt {

  private final AtomicLong remaining;
  private final AtomicLong splitsRemaining;
  private final IntSupplier supplier;

  /**
   * Create an instance.
   *
   * @param size The maximum number of values to output.
   * @param supplier The supplier to wrap.
   */
  public IntSupplierSpliterator(final long size, final IntSupplier supplier) {
    this(new AtomicLong(size), new AtomicLong(Long.SIZE - Long.numberOfLeadingZeros(size)),
        supplier);
  }

  /** Used to share the AtomicLong between partitions. */
  private IntSupplierSpliterator(final AtomicLong remaining, final AtomicLong splitsRemaining,
      final IntSupplier supplier) {
    this.remaining = remaining;
    this.splitsRemaining = splitsRemaining;
    this.supplier = supplier;
  }

  @SuppressWarnings("override.return.invalid") // actually is nullable in the interface
  @Override
  public @Nullable OfInt trySplit() {
    return ((splitsRemaining.getAndDecrement() <= 0) || (remaining.get() <= 0))
        ? null
        : new IntSupplierSpliterator(remaining, splitsRemaining, supplier);
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
