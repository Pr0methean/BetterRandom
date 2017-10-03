package io.github.pr0methean.betterrandom.util.spliterator;

import java.util.Spliterator.OfDouble;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * An unordered, concurrent {@link OfDouble} that invokes a {@link DoubleSupplier} to get its values
 * and has a preset size.
 */
public class DoubleSupplierSpliterator extends
    AbstractSupplierSpliterator<DoubleSupplier, DoubleConsumer, DoubleSupplierSpliterator> implements
    OfDouble {

  /**
   * Create an instance.
   *
   * @param size The maximum number of values to output.
   * @param supplier The supplier to wrap.
   */
  public DoubleSupplierSpliterator(final long size, final DoubleSupplier supplier) {
    super(size, supplier);
  }

  private DoubleSupplierSpliterator(AtomicLong remaining, AtomicLong splitsRemaining,
      DoubleSupplier doubleSupplier) {
    super(remaining, splitsRemaining, doubleSupplier);
  }

  @Override
  protected DoubleSupplierSpliterator internalSplit(AtomicLong remaining,
      AtomicLong splitsRemaining) {
    return new DoubleSupplierSpliterator(remaining, splitsRemaining, supplier);
  }

  @Override
  protected void internalSupplyAndAccept(DoubleConsumer action) {
    action.accept(supplier.getAsDouble());
  }
}
