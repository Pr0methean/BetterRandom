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
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class BaseEntropyCountingRandom extends BaseRandom implements
    EntropyCountingRandom {

  private static final long serialVersionUID = 1838766748070164286L;
  protected final AtomicLong entropyBits = new AtomicLong(0);
  @Nullable
  private RandomSeederThread thread;

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

  public void setSeederThread(RandomSeederThread thread) {
    thread.add(this);
    lock.lock();
    try {
      if (this.thread == thread) {
        return;
      }
      if (this.thread != null) {
        this.thread.remove(this);
      }
      this.thread = thread;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void setSeed(@UnknownInitialization(BaseRandom.class) BaseEntropyCountingRandom this,
      byte[] seed) {
    assert lock != null : "@AssumeAssertion(nullness)";
    assert entropyBits != null : "@AssumeAssertion(nullness)";
    super.setSeed(seed);
    entropyBits.updateAndGet(oldCount -> Math.max(oldCount, seed.length * 8));
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    assert entropyBits != null : "@AssumeAssertion(nullness)";
  }
  
  @Override
  public long entropyBits() {
    return entropyBits.get();
  }

  protected final void recordEntropySpent(long bits) {
    if (entropyBits.updateAndGet(oldCount -> Math.max(oldCount - bits, 0)) == 0
        && thread != null) {
      thread.asyncReseed(this);
    }
  }
}
