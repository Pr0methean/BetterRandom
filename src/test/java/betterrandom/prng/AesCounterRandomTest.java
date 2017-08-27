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
package betterrandom.prng;

import static betterrandom.prng.RandomTestUtils.DEFAULT_SEEDER;
import static betterrandom.prng.RandomTestUtils.assertEquivalentWhenSerializedAndDeserialized;
import static betterrandom.prng.RandomTestUtils.assertMonteCarloPiEstimateSane;
import static betterrandom.prng.RandomTestUtils.assertStandardDeviationSane;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import betterrandom.AllThreadsStackDumperThread;
import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.SeedException;
import java.io.IOException;
import java.lang.Thread.State;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * Unit test for the AES RNG.
 *
 * @author Daniel Dyer
 */
public class AesCounterRandomTest {

  private static final Logger LOG = Logger.getLogger(AesCounterRandomTest.class.getName());

  @Nullable
  private FileHandler logHandler;

  @BeforeSuite
  public void setUp() throws IOException {
    logHandler = new FileHandler("%h/javalog/log%u.%g.txt", 1_000_000, 10);
    Logger.getGlobal().addHandler(logHandler);
    if (AllThreadsStackDumperThread.INSTANCE.getState() == State.NEW) {
      AllThreadsStackDumperThread.INSTANCE.start();
    }
  }

  @AfterSuite
  public void tearDown() {
    if (logHandler != null) {
      logHandler.close();
    }
  }

  @Test(timeOut = 15000)
  public void testMaxSeedLengthOk() {
    assert AesCounterRandom.getMaxKeyLengthBytes() >= 16 :
        "Should allow a 16-byte key";
    assert AesCounterRandom.getMaxKeyLengthBytes() <= 32 :
        "Shouldn't allow a key longer than 32 bytes";
  }

  @Test(timeOut = 15000)
  public void testSerializableWithoutSeedInCounter()
      throws GeneralSecurityException, IOException, ClassNotFoundException, SeedException {
    // Serialise an RNG.
    AesCounterRandom rng = new AesCounterRandom(16);
    assertEquivalentWhenSerializedAndDeserialized(rng);
  }

  @Test(timeOut = 15000)
  public void testSerializableWithSeedInCounter()
      throws GeneralSecurityException, IOException, ClassNotFoundException, SeedException {
    // Serialise an RNG.
    AesCounterRandom rng = new AesCounterRandom(48);
    assertEquivalentWhenSerializedAndDeserialized(rng);
  }

  /**
   * Test to ensure that two distinct RNGs with the same seed return the same sequence of numbers.
   */
  @Test(timeOut = 15000)
  public void testRepeatability() throws GeneralSecurityException, SeedException {
    AesCounterRandom rng = new AesCounterRandom(48);
    // Create second RNG using same seed.
    AesCounterRandom duplicateRNG = new AesCounterRandom(rng.getSeed());
    assert rng.equals(duplicateRNG);
    assert RandomTestUtils
        .testEquivalence(rng, duplicateRNG, 1000) : "Generated sequences do not match.";
  }

  /**
   * Test to ensure that the output from the RNG is broadly as expected.  This will not detect the
   * subtle statistical anomalies that would be picked up by Diehard, but it provides a simple check
   * for major problems with the output.
   */
  @Test(timeOut = 15000, groups = "non-deterministic",
      dependsOnMethods = "testRepeatability")
  public void testDistribution() throws GeneralSecurityException, SeedException {
    AesCounterRandom rng = new AesCounterRandom(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
    assertMonteCarloPiEstimateSane(rng);
  }

  /**
   * Test to ensure that the output from the RNG is broadly as expected.  This will not detect the
   * subtle statistical anomalies that would be picked up by Diehard, but it provides a simple check
   * for major problems with the output.
   */
  @Test(timeOut = 30000, groups = "non-deterministic",
      dependsOnMethods = "testRepeatability")
  public void testStandardDeviation() throws GeneralSecurityException, SeedException {
    AesCounterRandom rng = new AesCounterRandom();
    // Expected standard deviation for a uniformly distributed population of values in the range 0..n
    // approaches n/sqrt(12).
    assertStandardDeviationSane(rng);
  }

  @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testSeedTooShort() throws GeneralSecurityException {
    new AesCounterRandom(new byte[]{1, 2, 3}); // Should throw an exception.
  }

  @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testSeedTooLong() throws GeneralSecurityException, SeedException {
    new AesCounterRandom(49); // Should throw an exception.
  }

  /**
   * RNG must not accept a null seed otherwise it will not be properly initialised.
   */
  @SuppressWarnings("argument.type.incompatible")
  @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testNullSeed() throws GeneralSecurityException {
    new AesCounterRandom((byte[]) null); // Should throw an exception.
  }

  @Test//(timeOut = 15000)
  public void testSetSeed() throws GeneralSecurityException, SeedException {
    // can't use a real SeedGenerator since we need longs, so use a Random
    Random masterRNG = new Random();
    long[] seeds = {masterRNG.nextLong(), masterRNG.nextLong(),
        masterRNG.nextLong(), masterRNG.nextLong()};
    long otherSeed = masterRNG.nextLong();
    AesCounterRandom[] rngs = {new AesCounterRandom(16), new AesCounterRandom(16)};
    for (int i = 0; i < 2; i++) {
      for (long seed : seeds) {
        AesCounterRandom rngReseeded = new AesCounterRandom(rngs[i].getSeed());
        assertTrue(rngReseeded.isSeeded());
        AesCounterRandom rngReseededOther = new AesCounterRandom(rngs[i].getSeed());
        rngReseeded.setSeed(seed);
        rngReseededOther.setSeed(otherSeed);
        assert !(rngs[i].equals(rngReseeded));
        assert !(rngReseededOther.equals(rngReseeded));
        assert rngs[i].nextLong() != rngReseeded.nextLong()
            : "setSeed had no effect";
        rngs[i] = rngReseeded;
      }
    }
    assert rngs[0].nextLong() != rngs[1].nextLong()
        : "RNGs converged after 4 setSeed calls";
  }

  @Test(timeOut = 60000)
  public void testEquals() throws GeneralSecurityException, ReflectiveOperationException {
    RandomTestUtils.doEqualsSanityChecks(AesCounterRandom.class.getConstructor());
  }

  @Test(timeOut = 30000)
  public void testHashCode() throws Exception {
    assert RandomTestUtils.testHashCodeDistribution(AesCounterRandom.class.getConstructor())
        : "Too many hashCode collisions";
  }

  @Test(timeOut = 15000)
  public void testReseeding() throws Exception {
    BaseEntropyCountingRandom rng = new AesCounterRandom();
    byte[] oldSeed = rng.getSeed();
    rng.setSeederThread(DEFAULT_SEEDER);
    rng.nextBytes(new byte[20000]);
    Thread.sleep(1500);
    byte[] newSeed = rng.getSeed();
    assertFalse(Arrays.equals(oldSeed, newSeed));
  }
}
