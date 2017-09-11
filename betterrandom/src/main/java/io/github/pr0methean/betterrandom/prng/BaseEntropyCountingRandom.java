package io.github.pr0methean.betterrandom.prng;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * <p>Abstract BaseEntropyCountingRandom class.</p>
 *
 * @author ubuntu
 * @version $Id: $Id
 */
public abstract class BaseEntropyCountingRandom extends BaseRandom implements
    EntropyCountingRandom {

  private static final long serialVersionUID = 1838766748070164286L;
  private static final LogPreFormatter LOG = new LogPreFormatter(BaseEntropyCountingRandom.class);
  protected AtomicLong entropyBits;
  protected @Nullable RandomSeederThread seederThread;

  /**
   * <p>Constructor for BaseEntropyCountingRandom.</p>
   *
   * @param seedLength a int.
   * @throws io.github.pr0methean.betterrandom.seed.SeedException if any.
   */
  @EnsuresNonNull("entropyBits")
  public BaseEntropyCountingRandom(final int seedLength) throws SeedException {
    super(seedLength);
    entropyBits = new AtomicLong(0);
  }

  /**
   * <p>Constructor for BaseEntropyCountingRandom.</p>
   *
   * @param seedGenerator a {@link io.github.pr0methean.betterrandom.seed.SeedGenerator}
   *     object.
   * @param seedLength a int.
   * @throws io.github.pr0methean.betterrandom.seed.SeedException if any.
   */
  @EnsuresNonNull("entropyBits")
  public BaseEntropyCountingRandom(final SeedGenerator seedGenerator, final int seedLength)
      throws SeedException {
    super(seedGenerator, seedLength);
    entropyBits = new AtomicLong(0);
  }

  /**
   * <p>Constructor for BaseEntropyCountingRandom.</p>
   *
   * @param seed an array of byte.
   */
  @EnsuresNonNull("entropyBits")
  public BaseEntropyCountingRandom(final byte[] seed) {
    super(seed);
    entropyBits = new AtomicLong(0);
  }

  /** {@inheritDoc} */
  @Override
  protected boolean withProbabilityInternal(final double probability) {
    final boolean result = super.withProbabilityInternal(probability);
    // Random.nextDouble() uses 53 bits, but we're only outputting 1, so credit the rest back
    // TODO: Maybe track fractional bits of entropy in a fixed-point form?
    recordEntropySpent(-52);
    return result;
  }

  /** {@inheritDoc} */
  @Override
  public ToStringHelper addSubclassFields(final ToStringHelper original) {
    return addSubSubclassFields(original
        .add("entropyBits", entropyBits.get())
        .add("seederThread", seederThread));
  }

  /**
   * <p>addSubSubclassFields.</p>
   *
   * @param original a {@link com.google.common.base.MoreObjects.ToStringHelper} object.
   * @return a {@link com.google.common.base.MoreObjects.ToStringHelper} object.
   */
  protected abstract ToStringHelper addSubSubclassFields(ToStringHelper original);

  /**
   * <p>Setter for the field <code>seederThread</code>.</p>
   *
   * @param thread a {@link io.github.pr0methean.betterrandom.seed.RandomSeederThread} object.
   */
  @SuppressWarnings("ObjectEquality")
  public void setSeederThread(@org.jetbrains.annotations.Nullable final RandomSeederThread thread) {
    if (thread != null) {
      thread.add(this);
    }
    lock.lock();
    try {
      if (this.seederThread == thread) {
        return;
      }
      if (this.seederThread != null) {
        this.seederThread.remove(this);
      }
      this.seederThread = thread;
    } finally {
      lock.unlock();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void setSeed(final byte[] seed) {
    super.setSeed(seed);
    entropyBits.updateAndGet(
        oldCount -> Math.max(oldCount, Math.min(seed.length, getNewSeedLength()) * 8));
  }

  /** {@inheritDoc} */
  @Override
  protected void setSeedInternal(@UnknownInitialization(Random.class)BaseEntropyCountingRandom this,
      final byte[] seed) {
    super.setSeedInternal(seed);
    entropyBits = new AtomicLong(Math.min(seed.length, getNewSeedLength()) * 8);
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    assert entropyBits != null : "@AssumeAssertion(nullness)";
  }

  /** {@inheritDoc} */
  @Override
  public long entropyBits() {
    return entropyBits.get();
  }

  /**
   * <p>recordEntropySpent.</p>
   *
   * @param bits a long.
   */
  protected final void recordEntropySpent(final long bits) {
    if (entropyBits.updateAndGet(oldCount -> Math.max(oldCount - bits, 0)) == 0
        && seederThread != null) {
      seederThread.asyncReseed(this);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void readObjectNoData() throws InvalidObjectException {
    LOG.warn("readObjectNoData() invoked; assuming initial entropy is equal to seed length!");
    super.readObjectNoData(); // TODO: Is this call redundant?
    entropyBits = new AtomicLong(seed.length * 8);
  }

  /** {@inheritDoc} */
  @Override
  public abstract int getNewSeedLength(@UnknownInitialization BaseEntropyCountingRandom this);
}
