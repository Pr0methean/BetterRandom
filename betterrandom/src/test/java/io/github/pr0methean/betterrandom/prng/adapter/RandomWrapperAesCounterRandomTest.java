package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.assertSame;

import com.google.common.collect.ImmutableList;
import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

@Test(testName = "RandomWrapper:AesCounterRandom") public class RandomWrapperAesCounterRandomTest
    extends RandomWrapperAbstractTest<RandomWrapper<AesCounterRandom>> {

  @Override @Test public void testThreadSafetySetSeed() {
    testThreadSafetyVsCrashesOnly(30,
        ImmutableList.of(NEXT_LONG, NEXT_INT, NEXT_DOUBLE, NEXT_GAUSSIAN, setSeed, setWrapped));
  }

  @SuppressWarnings("rawtypes") @Override protected Class<RandomWrapper> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override @Test(enabled = false) public void testAllPublicConstructors() {
    // No-op: redundant to super insofar as it works.
  }

  @Override protected RandomWrapper<AesCounterRandom> createRng() throws SeedException {
    return new RandomWrapper<>(new AesCounterRandom(getTestSeedGenerator()));
  }

  @Override protected RandomWrapper<AesCounterRandom> createRng(final byte[] seed) throws SeedException {
    return new RandomWrapper<>(new AesCounterRandom(seed));
  }

  @Test public void testGetWrapped() {
    assertSame(createRng().getWrapped().getClass(), AesCounterRandom.class);
  }
}
