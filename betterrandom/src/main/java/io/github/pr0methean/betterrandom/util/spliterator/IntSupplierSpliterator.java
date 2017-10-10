package io.github.pr0methean.betterrandom.util.spliterator;

import java.util.Spliterator.OfInt;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * An unordered, concurrent {@link OfInt} that invokes an {@link IntSupplier} to get its values. Has
 * a preset size if it isn't the result of a {@link #trySplit()}, but shares its total size with the
 * other splits if it is.
 *
 * @author Chris Hennick
 */
public class IntSupplierSpliterator extends
    AbstractSupplierSpliterator<IntSupplier, IntConsumer, IntSupplierSpliterator> implements OfInt {

  /**
   * Create an instance.
   *
   * @param size The maximum number of values to output.
   * @param supplier The supplier to wrap.
   */
  public IntSupplierSpliterator(final long size, final IntSupplier supplier) {
    super(size, supplier);
  }

  /** Used to share the AtomicLong between partitions. */
  private IntSupplierSpliterator(final AtomicLong remaining, final AtomicLong splitsRemaining,
      final IntSupplier supplier) {
    super(remaining, splitsRemaining, supplier, false);
  }

  @Override
  protected IntSupplierSpliterator internalSplit(final AtomicLong remaining,
      final AtomicLong splitsRemaining) {
    return new IntSupplierSpliterator(remaining, splitsRemaining, supplier);
  }

  @Override
  protected void internalSupplyAndAccept(final IntConsumer action) {
    action.accept(supplier.getAsInt());
  }
}
