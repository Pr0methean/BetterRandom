package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.assertSame;

import com.google.common.collect.ImmutableList;
import io.github.pr0methean.betterrandom.NamedFunction;
import io.github.pr0methean.betterrandom.TestUtils;
import io.github.pr0methean.betterrandom.prng.AbstractLargeSeedRandomTest;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import org.testng.annotations.Test;

@Test(testName = "RandomWrapper:SecureRandom") public class RandomWrapperSecureRandomTest
    extends AbstractLargeSeedRandomTest {

  private static final SecureRandom SEED_GEN = new SecureRandom();
  private static final NamedFunction<Random, Double> SET_WRAPPED = new NamedFunction<>(random -> {
    ((RandomWrapper) random).setWrapped(new SecureRandom());
    return 0.0;
  }, "setWrapped");

  private static RandomWrapper createRngInternal() {
    try {
      return new RandomWrapper(SecureRandom.getInstance("SHA1PRNG"));
    } catch (final NoSuchAlgorithmException e) {
      throw TestUtils.fail("NoSuchAlgorithmException should not occur for SHA1PRNG", e);
    }
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override protected Map<Class<?>, Object> constructorParams() {
    final Map<Class<?>, Object> params = super.constructorParams();
    params.put(Random.class, new SecureRandom());
    return params;
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

  @Override protected RandomWrapper createRng() throws SeedException {
    final RandomWrapper wrapper = createRngInternal();
    wrapper.setSeed(SEED_GEN.nextLong());
    return wrapper;
  }

  @Override protected RandomWrapper createRng(final byte[] seed) throws SeedException {
    final RandomWrapper wrapper = createRngInternal();
    wrapper.setSeed(seed);
    return wrapper;
  }

  /**
   * Assertion-free because SecureRandom isn't necessarily reproducible.
   */
  @Override @Test public void testThreadSafety() {
    testThreadSafetyVsCrashesOnly(30,
        ImmutableList.of(NEXT_LONG, NEXT_INT, NEXT_DOUBLE, NEXT_GAUSSIAN, SET_WRAPPED));
  }

  @Test public void testGetWrapped() {
    assertSame(createRng().getWrapped().getClass(), SecureRandom.class);
  }
}
