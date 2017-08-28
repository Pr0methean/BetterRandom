package betterrandom.prng;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.atomic.AtomicLong;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

public abstract class BaseEntropyCountingRandom extends BaseRandom {

  private static final long serialVersionUID = 1838766748070164286L;
  protected final AtomicLong entropyBits = new AtomicLong(0);

  @EnsuresNonNull("entropyBits")
  public BaseEntropyCountingRandom(byte[] seed) {
    super(seed);
  }

  @EnsuresNonNull({"seed", "lock", "entropyBits"})
  @Override
  protected void checkedReadObject(@UnknownInitialization BaseEntropyCountingRandom this,
      ObjectInputStream in)
      throws IOException, ClassNotFoundException {
    super.checkedReadObject(in);
    assert entropyBits != null : "@AssumeAssertion(nullness)";
  }

  @Override
  public void setSeed(@UnknownInitialization(BaseRandom.class)BaseEntropyCountingRandom this,
      byte[] seed) {
    assert lock != null : "@AssumeAssertion(nullness)";
    assert entropyBits != null : "@AssumeAssertion(nullness)";
    super.setSeed(seed);
    entropyBits.updateAndGet(oldCount -> Math.max(oldCount, seed.length * 8));
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    super.checkedReadObject(in);
    assert entropyBits != null : "@AssumeAssertion(nullness)";
  }

  protected final void recordEntropySpent(long bits) {
    entropyBits.updateAndGet(oldCount -> Math.max(oldCount - bits, 0));
  }
}
