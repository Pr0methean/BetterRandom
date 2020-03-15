package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.prng.Pcg64Random;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.Serializable;
import java.util.function.Supplier;
import org.testng.annotations.Test;

@Test(testName = "ThreadLocalRandomWrapper") public class ThreadLocalRandomWrapperPcg64RandomTest
    extends ThreadLocalRandomWrapperTest<Pcg64Random> {

  public ThreadLocalRandomWrapperPcg64RandomTest() {
    super(createSupplier());
  }

  private static Supplier<Pcg64Random> createSupplier() {
    final SeedGenerator seedGenerator = getTestSeedGenerator();
    return (Supplier<Pcg64Random> & Serializable) (() -> new Pcg64Random(seedGenerator));
  }


}
