package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.TestUtils.assertGreaterOrEqual;

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.FailingSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;
import org.testng.annotations.Test;

public class ReseedingThreadLocalRandomWrapperTest extends ThreadLocalRandomWrapperTest {
  
  @Override public void testWrapLegacy() throws SeedException {
    ReseedingThreadLocalRandomWrapper
        .wrapLegacy(Random::new, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR).nextInt();
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return ReseedingThreadLocalRandomWrapper.class;
  }

  @Override @Test public void testReseeding() {
    final BaseRandom rng =
        new ReseedingThreadLocalRandomWrapper(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR,
            (Serializable & Supplier<BaseRandom>) MersenneTwisterRandom::new);
    rng.nextLong();
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    final byte[] oldSeed = rng.getSeed();
    try {
      byte[] newSeed;
      do {
        rng.nextLong();
        Thread.sleep(100);
        newSeed = rng.getSeed();
      } while (Arrays.equals(newSeed, oldSeed));
      assertGreaterOrEqual(rng.getEntropyBits(), newSeed.length * 8L - 1);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override protected BaseRandom createRng() throws SeedException {
    return new ReseedingThreadLocalRandomWrapper(FailingSeedGenerator.FAILING_SEED_GENERATOR,
        (Serializable & Supplier<BaseRandom>) MersenneTwisterRandom::new);
  }
}
