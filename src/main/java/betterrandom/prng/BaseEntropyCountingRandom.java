package betterrandom.prng;

import betterrandom.EntropyCountingRandom;
import betterrandom.seed.RandomSeederThread;
import betterrandom.seed.SeedException;
import betterrandom.seed.SeedGenerator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.atomic.AtomicLong;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public abstract class BaseEntropyCountingRandom extends BaseRandom implements
    EntropyCountingRandom {

  private static final long serialVersionUID = 1838766748070164286L;
  protected AtomicLong entropyBits = new AtomicLong(0);

  @EnsuresNonNull("entropyBits")
  public BaseEntropyCountingRandom(int seedLength) throws SeedException {
    super(seedLength);
  }

  @EnsuresNonNull("entropyBits")
  public BaseEntropyCountingRandom(SeedGenerator seedGenerator, int seedLength)
      throws SeedException {
    super(seedGenerator, seedLength);
  }

  @EnsuresNonNull("entropyBits")
  public BaseEntropyCountingRandom(byte[] seed) {
    super(seed);
  }

  @SuppressWarnings("contracts.precondition.override.invalid")
  @Override
  @RequiresNonNull({"entropyBits", "lock", "this.seed"})
  public void setSeed(@UnknownInitialization BaseEntropyCountingRandom this, byte[] seed) {
    super.setSeed(seed);
    entropyBits.updateAndGet(oldCount -> Math.max(oldCount, seed.length * 8));
  }

  @Override
  public long entropyOctets() {
    return entropyBits.get();
  }

  protected final void recordEntropySpent(long bits) {
    if (entropyBits.addAndGet(-bits) <= 0) {
      RandomSeederThread.asyncReseed(this);
    }
  }
}
