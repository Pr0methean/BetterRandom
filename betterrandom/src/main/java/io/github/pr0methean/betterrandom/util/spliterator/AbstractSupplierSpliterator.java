package io.github.pr0methean.betterrandom.util.spliterator;

import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.SIZED;

import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An unordered, concurrent spliterator (possibly unboxed) that invokes a supplier (possibly
 * unboxed) to get its values and has a preset size.
 *
 * @param <TSupplier> A specification or unboxed-primitive equivalent of {@link
 *     java.util.function.Supplier}.
 * @param <TConsumer> A specification or unboxed-primitive equivalent of {@link
 *     java.util.function.Consumer} The consumer type that {@link #tryAdvance(Object)} receives.
 * @param <TSplitInto> The return type of {@link #trySplit()}. Should be a self-reference (i.e.
 *     {@code ConcreteSupplierSpliterator extends AbstractSupplierSpliterator<T, U,
 *     ConcreteSupplierSpliterator>}).
 * @author Chris Hennick
 */
public abstract class AbstractSupplierSpliterator<TSupplier, TConsumer, TSplitInto> {

  /** The supplier of the output values. */
  protected final TSupplier supplier;
  private final AtomicLong remaining;
  private final AtomicLong splitsRemaining;
  private final AtomicBoolean sized = new AtomicBoolean();

  /**
   * Create an instance.
   *
   * @param size The maximum number of values to output.
   * @param supplier The supplier to wrap.
   */
  public AbstractSupplierSpliterator(final long size, final TSupplier supplier) {
    this(new AtomicLong(size), new AtomicLong(Long.SIZE - Long.numberOfLeadingZeros(size)),
        supplier, true);
  }

  /**
   * Used to share the AtomicLongs between partitions.
   *
   * @param remaining An {@link AtomicLong} (shared between splits) that stores how many more
   *     items will be output by {@link #tryAdvance(Object)}.
   * @param splitsRemaining An {@link AtomicLong} (shared between splits) that stores how many
   *     more times the {@link #trySplit()} command will return non-null.
   * @param supplier The supplier to wrap.
   * @param sized Should be true if this is the root spliterator, and false otherwise (since the
   *     root spliterator stores the total size, and concurrent splits may compete to exhaust it).
   */
  protected AbstractSupplierSpliterator(final AtomicLong remaining,
      final AtomicLong splitsRemaining,
      final TSupplier supplier, boolean sized) {
    this.remaining = remaining;
    this.splitsRemaining = splitsRemaining;
    this.supplier = supplier;
    this.sized.set(sized);
  }

  /**
   * @return a descendant spliterator, or null if this spliterator refuses to be split any further.
   * @see Spliterator#trySplit()
   */
  @SuppressWarnings("override.return.invalid") // actually is nullable in the interface
  public @Nullable TSplitInto trySplit() {
    if ((splitsRemaining.getAndDecrement() <= 0) || (remaining.get() <= 0)) {
      return null;
    } else {
      sized.set(false); // No longer have exact size, since child will be pulling off of us
      return internalSplit(remaining, splitsRemaining);
    }
  }

  /**
   * Splits off a new instance that uses the same Supplier, and shares its counts of remaining items
   * and remaining permitted splits with this one. Doesn't decrement the number of remaining
   * permitted splits (that's done in {@link #trySplit()}). Should wrap a constructor that
   * ultimately calls {@link #AbstractSupplierSpliterator(AtomicLong, AtomicLong, Object, boolean)
   * AbstractSupplierSpliterator(remaining, splitsRemaining, supplier, false)}.
   *
   * @param remaining An {@link AtomicLong} (shared between splits) that stores how many more
   *     items will be output by {@link #tryAdvance(Object)}.
   * @param splitsRemaining An {@link AtomicLong} (shared between splits) that stores how many
   *     more times the {@link #trySplit()} command will return non-null.
   * @return a descendant spliterator.
   */
  protected abstract TSplitInto internalSplit(AtomicLong remaining, AtomicLong splitsRemaining);

  /**
   * Returns an upper bound on the number of remaining items.
   * @return the total number of items remaining in the root spliterator, and thus an upper bound on
   *     the number available to any one descendant.
   * @see Spliterator#estimateSize()
   */
  public long estimateSize() {
    return remaining.get();
  }

  /**
   * Returns {@link Spliterator#IMMUTABLE} | {@link Spliterator#NONNULL}. Also returns {@link Spliterator#SIZED}
   *     if this spliterator has never been split and doesn't result from a split.
   * @return The characteristics as defined by {@link Spliterator#characteristics()}.
   */
  public int characteristics() {
    return IMMUTABLE | NONNULL | (sized.get() ? SIZED : 0);
  }

  /**
   * @param action the consumer that will receive the next output value if there is one.
   * @return true if an output was produced and consumed.
   * @see Spliterator#tryAdvance(java.util.function.Consumer)
   */
  public boolean tryAdvance(final TConsumer action) {
    if (remaining.decrementAndGet() >= 0) {
      internalSupplyAndAccept(action);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Supplies the next output value to {@code action}. Should be equivalent to {@code
   * action.accept(supplier.get())}.
   *
   * @param action the consumer that will receive the next output value if there is one.
   */
  protected abstract void internalSupplyAndAccept(TConsumer action);
}
