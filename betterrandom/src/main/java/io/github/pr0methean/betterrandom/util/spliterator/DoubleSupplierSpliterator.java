package io.github.pr0methean.betterrandom.util.spliterator;

import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import java.util.Spliterator.OfDouble;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An unordered, concurrent {@link OfDouble} that invokes a {@link DoubleSupplier} to get its values
 * and has a preset size.
 */
public class DoubleSupplierSpliterator implements OfDouble {

  private static final LogPreFormatter LOG = new LogPreFormatter(DoubleSupplierSpliterator.class);
  private final AtomicLong remaining;
  private final AtomicLong splitsRemaining;
  private final DoubleSupplier supplier;

  /**
   * Create an instance.
   *
   * @param size The maximum number of values to output.
   * @param supplier The supplier to wrap.
   */
  public DoubleSupplierSpliterator(final long size, final DoubleSupplier supplier) {
    this(new AtomicLong(size), new AtomicLong(Long.SIZE - Long.numberOfLeadingZeros(size)),
        supplier);
  }

  /** Used to share the AtomicLongs between partitions. */
  private DoubleSupplierSpliterator(final AtomicLong remaining, final AtomicLong splitsRemaining,
      final DoubleSupplier supplier) {
    this.remaining = remaining;
    this.splitsRemaining = splitsRemaining;
    this.supplier = supplier;
  }

  @SuppressWarnings("override.return.invalid") // actually is nullable in the interface
  @Override
  public @Nullable OfDouble trySplit() {
    //LOG.logStackTrace(Level.INFO, Thread.currentThread().getStackTrace());
    return ((splitsRemaining.getAndDecrement() <= 0) || (remaining.get() <= 0))
        ? null
        : new DoubleSupplierSpliterator(remaining, splitsRemaining, supplier);
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
  public boolean tryAdvance(final DoubleConsumer action) {
    //LOG.logStackTrace(Level.INFO, Thread.currentThread().getStackTrace());
    if (remaining.decrementAndGet() >= 0) {
      action.accept(supplier.getAsDouble());
      return true;
    } else {
      return false;
    }
  }
}
