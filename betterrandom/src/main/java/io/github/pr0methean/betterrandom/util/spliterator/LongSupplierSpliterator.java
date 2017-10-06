package io.github.pr0methean.betterrandom.util.spliterator;

import java.util.Spliterator.OfLong;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/**
 * An unordered, concurrent {@link OfLong} that invokes a {@link LongSupplier} to get its values.
 * Has a preset size if it isn't the result of a {@link #trySplit()}, but shares its total size with
 * the other splits if it is.
 *
 * @author Chris Hennick
 */
public class LongSupplierSpliterator extends
    AbstractSupplierSpliterator<LongSupplier, LongConsumer, LongSupplierSpliterator> implements
    OfLong {

  /**
   * Create an instance.
   *
   * @param size The maximum number of values to output.
   * @param supplier The supplier to wrap.
   */
  public LongSupplierSpliterator(final long size, final LongSupplier supplier) {
    super(size, supplier);
  }

  /** Used to share the AtomicLong between partitions. */
  private LongSupplierSpliterator(final AtomicLong remaining, final AtomicLong splitsRemaining,
      final LongSupplier supplier) {
    super(remaining, splitsRemaining, supplier, false);
  }

  @Override
  protected LongSupplierSpliterator internalSplit(AtomicLong remaining,
      AtomicLong splitsRemaining) {
    return new LongSupplierSpliterator(remaining, splitsRemaining, supplier);
  }

  @Override
  protected void internalSupplyAndAccept(LongConsumer action) {
    action.accept(supplier.getAsLong());
  }
}
