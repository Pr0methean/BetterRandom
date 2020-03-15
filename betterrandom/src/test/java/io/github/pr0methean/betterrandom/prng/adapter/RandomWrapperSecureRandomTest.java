package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.assertSame;

import com.google.common.testing.SerializableTester;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.seed.PseudorandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.testng.annotations.Test;

@Test(testName = "RandomWrapper:SecureRandom") public class RandomWrapperSecureRandomTest
    extends RandomWrapperAbstractTest<RandomWrapper<SecureRandom>, SecureRandom> {

  @SuppressWarnings("rawtypes")
  @Override protected Class<RandomWrapper> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override protected Map<Class<?>, Object> constructorParams() {
    final Map<Class<?>, Object> params = super.constructorParams();
    params.put(Random.class, new SecureRandom());
    return params;
  }

  @Override public void testRandomSeederIntegration() {
    final SeedGenerator seedGenerator = new PseudorandomSeedGenerator(new Random(),
        UUID.randomUUID().toString());
    final BaseRandom rng = createRng();
    RandomTestUtils.checkReseeding(seedGenerator, rng, true);
  }

  /**
   * {@link SecureRandom#setSeed(byte[])} has no length restriction, so disinherit {@link
   * Test#expectedExceptions()}.
   */
  @Override @Test public void testSeedTooLong() throws GeneralSecurityException, SeedException {
    super.testSeedTooLong();
  }

  /**
   * {@link SecureRandom#setSeed(byte[])} has no length restriction, so disinherit {@link
   * Test#expectedExceptions()}.
   */
  @Override @Test public void testSeedTooShort() throws SeedException {
    super.testSeedTooShort();
  }

  @Override public void testThreadSafety() {
    checkThreadSafetyVsCrashesOnly(30, functionsForThreadSafetyTest, functionsForThreadSafetyTest);
  }

  @Override @Test(enabled = false) public void testNullSeed() throws SeedException {
    // No-op.
  }

  /**
   * Only test for crashes, since {@link SecureRandom#setSeed(long)} doesn't completely replace the
   * existing seed.
   */
  @Override public void testSetSeedAfterNextLong() throws SeedException {
    final BaseRandom prng = createRng();
    prng.nextLong();
    prng.setSeed(getTestSeedGenerator().generateSeed(8));
    prng.nextLong();
  }

  /**
   * Only test for crashes, since {@link SecureRandom#setSeed(long)} doesn't completely replace the
   * existing seed.
   */
  @Override public void testSetSeedAfterNextInt() throws SeedException {
    final BaseRandom prng = createRng();
    prng.nextInt();
    prng.setSeed(getTestSeedGenerator().generateSeed(8));
    prng.nextInt();
  }

  @Override @Test(enabled = false) public void testRepeatability() throws SeedException {
    // No-op.
  }

  @Override @Test(enabled = false) public void testRepeatabilityNextGaussian()
      throws SeedException {
    // No-op.
  }

  /**
   * Assertion-free because SecureRandom itself isn't reproducible after a serialization round trip.
   * @throws SeedException
   */
  @Override public void testSerializable() throws SeedException {
    assertSame(SerializableTester.reserialize(createRng()).getWrapped().getClass(), SecureRandom.class,
        "Not the same type after serialization");
  }

  @Override protected RandomWrapper<SecureRandom> createRng() throws SeedException {
    return createRng(getTestSeedGenerator().generateSeed(32));
  }

  @Override protected RandomWrapper<SecureRandom> createRng(final byte[] seed) throws SeedException {
    return RandomWrapper.wrapSecureRandom(seed);
  }

  @Override protected SecureRandom createWrappedPrng() {
    try {
      return SecureRandom.getInstance("SHA1PRNG");
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }

  @Override protected SecureRandom createWrappedPrng(byte[] seed) {
    SecureRandom out = createWrappedPrng();
    out.setSeed(seed);
    return out;
  }

  @Test public void testGetWrapped() {
    assertSame(createRng().getWrapped().getClass(), SecureRandom.class);
  }
}
