// ============================================================================
//   Copyright 2006-2012 Daniel W. Dyer
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
// ============================================================================
package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import com.google.common.collect.ImmutableList;
import io.github.pr0methean.betterrandom.NamedFunction;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import org.testng.annotations.Test;

/**
 * Unit test for the JDK RNG.
 *
 * @author Daniel Dyer
 * @author Chris Hennick
 */
@Test(testName = "RandomWrapper") public class RandomWrapperRandomTest
    extends RandomWrapperAbstractTest<RandomWrapper<Random>, Random> {

  protected static final NamedFunction<RandomWrapper<? super Random>,
      Double> SET_WRAPPED = new NamedFunction<>(random -> {
    random.setWrapped(new Random());
    return 0.0;
  }, "setWrapped");

  @SuppressWarnings("rawtypes")
  @Override protected Class<? extends RandomWrapper> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override protected Map<Class<?>, Object> constructorParams() {
    final Map<Class<?>, Object> params = super.constructorParams();
    params.put(Random.class, new Random());
    return params;
  }

  /**
   * Assertion-free with respect to the long/double methods because, contrary to its contract to be
   * thread-safe, {@link Random#nextLong()} is not transactional. Rather, it uses two subroutine
   * calls that can interleave with calls from other threads.
   */
  @Override public void testThreadSafety() {
    checkThreadSafety(ImmutableList.of(NEXT_INT), Collections.emptyList());
    checkThreadSafetyVsCrashesOnly(30,
        ImmutableList.of(NEXT_LONG, NEXT_INT, NEXT_DOUBLE, NEXT_GAUSSIAN, SET_WRAPPED));
  }

  @Override public void testSetSeedLong() throws SeedException {
    final BaseRandom rng = createRng();
    final BaseRandom rng2 = createRng();
    checkSetSeedLong(rng, rng2);
  }

  /**
   * Test to ensure that two distinct RNGs with the same seed return the same sequence of numbers.
   */
  @Override @Test(timeOut = 30_000) public void testRepeatability() throws SeedException {
    // Create an RNG using the default seeding strategy.
    final RandomWrapper<Random> rng = RandomWrapper.wrapJavaUtilRandom(getTestSeedGenerator());
    // Create second RNG using same seed.
    final RandomWrapper<Random> duplicateRNG = RandomWrapper.wrapJavaUtilRandom(rng.getSeed());
    RandomTestUtils.assertEquivalent(rng, duplicateRNG, 200, "Generated sequences do not match.");
  }

  @Override protected Random createWrappedPrng() {
    return new Random();
  }

  @Override protected Random createWrappedPrng(byte[] seed) {
    assertEquals(seed.length, Long.BYTES, "Wrong seed length");
    return new Random(BinaryUtils.convertBytesToLong(seed));
  }

  @Override protected RandomWrapper<Random> createRng() throws SeedException {
    return RandomWrapper.wrapJavaUtilRandom(getTestSeedGenerator());
  }

  @Override protected RandomWrapper<Random> createRng(final byte[] seed) throws SeedException {
    return RandomWrapper.wrapJavaUtilRandom(seed);
  }

  @Test public void testGetWrapped() {
    assertSame(createRng().getWrapped().getClass(), Random.class);
  }
}
