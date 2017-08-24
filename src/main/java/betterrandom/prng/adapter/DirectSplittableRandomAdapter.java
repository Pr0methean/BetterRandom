package betterrandom.prng.adapter;

import betterrandom.util.BinaryUtils;
import java.util.SplittableRandom;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public abstract class DirectSplittableRandomAdapter extends BaseSplittableRandomAdapter {

  private static final long serialVersionUID = 4273652147052638879L;
  protected transient SplittableRandom underlying; // a SplittableRandom is not Serializable

  @SuppressWarnings("OverriddenMethodCallDuringObjectConstruction")
  public DirectSplittableRandomAdapter(byte[] seed) {
    super(seed);
    initSubclassTransientFields();
  }

  @EnsuresNonNull("underlying")
  @RequiresNonNull({"seed", "lock"})
  @Override
  protected void initSubclassTransientFields(
      @UnknownInitialization DirectSplittableRandomAdapter this) {
    underlying = new SplittableRandom(
        BinaryUtils.convertBytesToLong(seed, 0));
    setSeed(seed);
  }
}
