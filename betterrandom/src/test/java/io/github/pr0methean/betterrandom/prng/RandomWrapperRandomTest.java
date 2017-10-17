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
package io.github.pr0methean.betterrandom.prng;

import com.google.common.collect.ImmutableMap;
import io.github.pr0methean.betterrandom.TestUtils;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import org.testng.annotations.Test;

/**
 * Unit test for the JDK RNG.
 *
 * @author Daniel Dyer
 */
public class RandomWrapperRandomTest extends BaseRandomTest {

  @Override
  protected Class<? extends BaseRandom> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override
  public void testAllPublicConstructors()
      throws SeedException, IllegalAccessException, InstantiationException, InvocationTargetException {
    final BaseRandom basePrng = createRng();
    final int seedLength = getNewSeedLength(basePrng);
    TestUtils.testAllPublicConstructors(getClassUnderTest(), ImmutableMap.of(
        int.class, seedLength,
        long.class, TEST_SEED,
        byte[].class, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(seedLength),
        SeedGenerator.class, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR,
        Random.class, new Random()
    ), BaseRandom::nextInt);
  }

  /**
   * Test to ensure that two distinct RNGs with the same seed return the same sequence of numbers.
   */
  @Override
  @Test(timeOut = 15000)
  public void testRepeatability() throws SeedException {
    // Create an RNG using the default seeding strategy.
    final RandomWrapper rng = new RandomWrapper();
    // Create second RNG using same seed.
    final RandomWrapper duplicateRNG = new RandomWrapper(rng.getSeed());
    assert RandomTestUtils
        .testEquivalence(rng, duplicateRNG, 1000) : "Generated sequences do not match.";
  }

  @Override
  protected BaseRandom createRng() throws SeedException {
    return new RandomWrapper();
  }

  @Override
  protected BaseRandom createRng(final byte[] seed) throws SeedException {
    return new RandomWrapper(seed);
  }
}
