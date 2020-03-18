package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.prng.Pcg64Random;
import io.github.pr0methean.betterrandom.seed.PseudorandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.SerializableSupplier;
import org.testng.annotations.Test;

@Test(testName = "ThreadLocalRandomWrapper") public class ThreadLocalRandomWrapperPcg64RandomTest
    extends ThreadLocalRandomWrapperTest<Pcg64Random> {

  public ThreadLocalRandomWrapperPcg64RandomTest() {
    super(createSupplier());
  }

  private static SerializableSupplier<Pcg64Random> createSupplier() {
    final SeedGenerator seedGenerator = new PseudorandomSeedGenerator();
    return () -> new Pcg64Random(seedGenerator);
  }


}
