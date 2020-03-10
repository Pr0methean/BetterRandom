package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.prng.CipherCounterRandomTest.checkInitialEntropyForCipher;
import static io.github.pr0methean.betterrandom.prng.CipherCounterRandomTest.checkSetSeedForCipher;
import static org.testng.Assert.assertSame;

import com.google.common.collect.ImmutableList;
import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

@Test(testName = "RandomWrapper:AesCounterRandom") public class RandomWrapperAesCounterRandomTest
    extends RandomWrapperAbstractTest<RandomWrapper<AesCounterRandom>, AesCounterRandom> {

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
    return new RandomWrapper<>(createWrappedPrng());
  }

  @Override protected AesCounterRandom createWrappedPrng() {
    return new AesCounterRandom(getTestSeedGenerator());
  }

  @Override public void testInitialEntropy() {
    checkInitialEntropyForCipher(this, new AesCounterRandom().getCounterSizeBytes());
  }

  @Override protected int getNewSeedLength() {
    return AesCounterRandom.MAX_SEED_LENGTH_BYTES;
  }

  @Override protected RandomWrapper<AesCounterRandom> createRng(final byte[] seed) throws SeedException {
    return new RandomWrapper<>(createWrappedPrng(seed));
  }

  @Override protected AesCounterRandom createWrappedPrng(byte[] seed) {
    return new AesCounterRandom(seed);
  }

  @Test public void testGetWrapped() {
    assertSame(createRng().getWrapped().getClass(), AesCounterRandom.class);
  }

  @Override @Test(timeOut = 40_000)
  public void testSetSeedAfterNextLong() throws SeedException {
    checkSetSeedForCipher(this);
  }

  @Override @Test(enabled = false) public void testSetSeedAfterNextInt() {
    // No-op.
  }
}
