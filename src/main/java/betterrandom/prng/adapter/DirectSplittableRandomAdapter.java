package betterrandom.prng.adapter;

import betterrandom.util.BinaryUtils;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.SplittableRandom;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public abstract class DirectSplittableRandomAdapter extends BaseSplittableRandomAdapter {

  private static final long serialVersionUID = 4273652147052638879L;
  protected transient SplittableRandom underlying; // a SplittableRandom is not Serializable

  public DirectSplittableRandomAdapter(byte[] seed) {
    super(seed);
    initSubclassTransientFields();
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    assert seed != null : "@AssumeAssertion(nullness)";
    assert lock != null : "@AssumeAssertion(nullness)";
    initSubclassTransientFields();
  }

  @EnsuresNonNull("underlying")
  @RequiresNonNull({"seed", "lock"})
  private void initSubclassTransientFields(
      @UnknownInitialization DirectSplittableRandomAdapter this) {
    underlying = new SplittableRandom(
        BinaryUtils.convertBytesToLong(seed, 0));
    setSeed(seed);
  }
}
