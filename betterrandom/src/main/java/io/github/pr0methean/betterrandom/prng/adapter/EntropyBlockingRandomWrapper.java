package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.prng.EntropyBlockingHelper;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeeder;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

/**
 * A {@link RandomWrapper} with the additional property that it won't return any output that would
 * take its entropy below a minimum amount, and will instead either wait to be reseeded by a {@link
 * RandomSeederThread} or, if none is installed, reseed itself on the calling thread with a {@link
 * SeedGenerator}. If neither is present, the caller is responsible for reseeding, and any call that
 * would reduce entropy below the minimum will throw {@link IllegalStateException}.
 */
public class EntropyBlockingRandomWrapper extends RandomWrapper {

  private static final long serialVersionUID = -853699062122154479L;
  private final EntropyBlockingHelper helper;
  private final AtomicReference<SeedGenerator> sameThreadSeedGen;

  public EntropyBlockingRandomWrapper(long minimumEntropy, SeedGenerator seedGenerator)
      throws SeedException {
    super(seedGenerator);
    sameThreadSeedGen = new AtomicReference<>(seedGenerator);
    helper = new EntropyBlockingHelper(minimumEntropy, sameThreadSeedGen, this, this.entropyBits,
        lock);
    helper.checkMaxOutputAtOnce();
  }

  public EntropyBlockingRandomWrapper(byte[] seed, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(seed);
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
    helper = new EntropyBlockingHelper(minimumEntropy, this.sameThreadSeedGen, this,
        this.entropyBits, lock);
    helper.checkMaxOutputAtOnce();
  }

  public EntropyBlockingRandomWrapper(long seed, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(seed);
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
    helper = new EntropyBlockingHelper(minimumEntropy, this.sameThreadSeedGen, this,
        this.entropyBits, lock);
    helper.checkMaxOutputAtOnce();
  }

  public EntropyBlockingRandomWrapper(Random wrapped, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(wrapped);
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
    helper = new EntropyBlockingHelper(minimumEntropy, this.sameThreadSeedGen, this,
        this.entropyBits, lock);
    helper.checkMaxOutputAtOnce();
  }

  @Override public void nextBytes(byte[] bytes) {
    for (int i = 0; i < bytes.length; i++) {
      debitEntropy(Byte.SIZE);
      bytes[i] = (byte) (nextInt(1 << Byte.SIZE));
    }
  }

  @Nullable public SeedGenerator getSameThreadSeedGen() {
    return sameThreadSeedGen.get();
  }

  public void setSameThreadSeedGen(@Nullable SeedGenerator newSeedGen) {
    helper.setSameThreadSeedGen(newSeedGen);
  }

  @Override protected void setSeedInternal(byte[] seed) {
    super.setSeedInternal(seed);
    if (helper != null) {
      helper.onSeedingStateChanged(true);
    }
  }

  @Override public void setRandomSeeder(@Nullable SimpleRandomSeeder randomSeeder) {
    super.setRandomSeeder(randomSeeder);
    helper.onSeedingStateChanged(false);
  }

  @Override protected void creditEntropyForNewSeed(int seedLength) {
    if (helper != null) {
      helper.creditEntropyForNewSeed(seedLength);
    }
  }

  @Override protected void debitEntropy(long bits) {
    helper.debitEntropy(bits);
  }

  @Override public void setSeed(long seed) {
    if (helper == null) {
      super.setSeed(seed);
      return;
    }
    lock.lock();
    try {
      super.setSeed(seed);
      helper.onSeedingStateChanged(true);
    } finally {
      lock.unlock();
    }
  }

  @Override public boolean needsReseedingEarly() {
    return helper.isWaitingOnReseed();
  }
}
