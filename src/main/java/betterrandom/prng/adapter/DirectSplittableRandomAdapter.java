package betterrandom.prng.adapter;

import betterrandom.util.BinaryUtils;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Random;
import java.util.SplittableRandom;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

public abstract class DirectSplittableRandomAdapter extends BaseSplittableRandomAdapter {

  private static final long serialVersionUID = 4273652147052638879L;
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  protected transient SplittableRandom underlying; // a SplittableRandom is not Serializable

  public DirectSplittableRandomAdapter(byte[] seed) {
    super(seed);
    setSeedInitial(seed);
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    setSeedInitial(seed);
  }

  @EnsuresNonNull({"this.seed", "underlying"})
  @Override
  public void setSeedInitial(@UnknownInitialization(Random.class) DirectSplittableRandomAdapter this,
      byte[] seed) {
    super.setSeedInitial(seed);
    underlying = new SplittableRandom(
        BinaryUtils.convertBytesToLong(seed, 0));
  }
}
