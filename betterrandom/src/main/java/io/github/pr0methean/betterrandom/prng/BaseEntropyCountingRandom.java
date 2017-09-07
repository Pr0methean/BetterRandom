package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class BaseEntropyCountingRandom extends BaseRandom implements
    EntropyCountingRandom {

  private static final long serialVersionUID = 1838766748070164286L;
  private static final LogPreFormatter LOG = new LogPreFormatter(BaseEntropyCountingRandom.class);
  protected AtomicLong entropyBits;
  @Nullable
  private RandomSeederThread thread;

  @EnsuresNonNull("entropyBits")
  public BaseEntropyCountingRandom(int seedLength) throws SeedException {
    super(seedLength);
    assert entropyBits != null : "@AssumeAssertion(nullness)";
  }

  @EnsuresNonNull("entropyBits")
  public BaseEntropyCountingRandom(SeedGenerator seedGenerator, int seedLength)
      throws SeedException {
    super(seedGenerator, seedLength);
    assert entropyBits != null : "@AssumeAssertion(nullness)";
  }

  @EnsuresNonNull("entropyBits")
  public BaseEntropyCountingRandom(byte[] seed) {
    super(seed);
    assert entropyBits != null : "@AssumeAssertion(nullness)";
  }

  @Override
  public ToStringHelper addSubclassFields(ToStringHelper original) {
    return addSubSubclassFields(original
        .add("entropyBits", entropyBits.get())
        .add("thread", thread));
  }

  protected abstract ToStringHelper addSubSubclassFields(ToStringHelper original);

  @SuppressWarnings("ObjectEquality")
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
  public void setSeed(byte[] seed) {
    super.setSeed(seed);
    entropyBits.updateAndGet(oldCount -> Math.max(oldCount, seed.length * 8));
  }

  @Override
  public void setSeedInternal(@UnknownInitialization(Random.class)BaseEntropyCountingRandom this,
      byte[] seed) {
    super.setSeedInternal(seed);
    entropyBits = new AtomicLong(seed.length * 8);
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

  @Override
  protected void readObjectNoData() throws InvalidObjectException {
    LOG.warn("readObjectNoData() invoked; assuming initial entropy is equal to seed length!");
    super.readObjectNoData(); // TODO: Is this call redundant?
    entropyBits = new AtomicLong(seed.length * 8);
  }
}
