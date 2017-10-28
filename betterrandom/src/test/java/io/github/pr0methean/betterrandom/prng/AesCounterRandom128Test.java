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

import static org.testng.Assert.assertTrue;

import io.github.pr0methean.betterrandom.SeekableRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.testng.annotations.Test;

/**
 * Unit test for the AES RNG.
 * @author Daniel Dyer
 */
public class AesCounterRandom128Test extends BaseRandomTest {

  private static final int ITERATIONS = 8;

  @Test
  public void testAdvanceForward() {
    SeekableRandom copy1 = (SeekableRandom) createRng();
    Random copy1AsRandom = (Random) copy1;
    SeekableRandom copy2 = (SeekableRandom) createRng(copy1.getSeed());
    Random copy2AsRandom = (Random) copy2;
    for (int i=0; i < ITERATIONS; i++) {
      copy1AsRandom.nextInt();
    }
    copy2.advance(ITERATIONS);
    RandomTestUtils.testEquivalence(copy1AsRandom, copy2AsRandom, 20);
  }

  @Test
  public void testAdvanceBackward() {
    SeekableRandom copy1 = (SeekableRandom) createRng();
    Random copy1AsRandom = (Random) copy1;
    SeekableRandom copy2 = (SeekableRandom) createRng(copy1.getSeed());
    Random copy2AsRandom = (Random) copy2;
    for (int i=0; i < ITERATIONS; i++) {
      copy1AsRandom.nextInt();
    }
    copy1.advance(-ITERATIONS);
    RandomTestUtils.testEquivalence(copy1AsRandom, copy2AsRandom, 20);
  }

  @Test(timeOut = 15000) public void testMaxSeedLengthOk() {
    assert AesCounterRandom.getMaxKeyLengthBytes() >= 16 : "Should allow a 16-byte key";
    assert AesCounterRandom.getMaxKeyLengthBytes() <= 32
        : "Shouldn't allow a key longer than 32 bytes";
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return AesCounterRandom.class;
  }

  @Override protected BaseRandom createRng() throws SeedException {
    return new AesCounterRandom(16);
  }

  @Override protected BaseRandom createRng(final byte[] seed) throws SeedException {
    return new AesCounterRandom(seed);
  }
}
