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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Random;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/**
 * <p>Java port of the <a href="http://home.southernct.edu/~pasqualonia1/ca/report.html"
 * target="_top">cellular automaton pseudorandom number generator</a> developed by Tony
 * Pasqualoni.</p>
 *
 * <p><em>NOTE: Instances of this class do not use the seeding mechanism inherited from {@link
 * Random}.  Calls to the {@link #setSeed(long)} method will have no effect.  Instead the seed must
 * be set by a constructor.</em></p>
 *
 * @author Tony Pasqualoni (original C version)
 * @author Daniel Dyer (Java port)
 */
public class CellularAutomatonRandom extends BaseEntropyCountingRandom {

  private static final int BITWISE_BYTE_TO_INT = 0x000000FF;
  private static final long serialVersionUID = 5959251752288589909L;
  private static final int SEED_SIZE_BYTES = 4;
  private static final int AUTOMATON_LENGTH = 2056;
  private transient int[] cells;

  public CellularAutomatonRandom() {
    this(SECURE_RANDOM.generateSeed(SEED_SIZE_BYTES));
  }

  /**
   * Creates an RNG and seeds it with the specified seed data.
   *
   * @param seed The seed data used to initialise the RNG.
   */
  public CellularAutomatonRandom(byte[] seed) {
    super(seed);
    initSubclassTransientFields();
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    checkedReadObject(in);
    initSubclassTransientFields();
  }

  @EnsuresNonNull("cells")
  @RequiresNonNull({"seed", "lock"})
  private void copySeedToCellsAndPreEvolve(@UnknownInitialization CellularAutomatonRandom this) {
    lock.lock();
    try {
      cells = new int[AUTOMATON_LENGTH];
      // Set initial cell states using seed.
      cells[AUTOMATON_LENGTH - 1] = seed[0] + 128;
      cells[AUTOMATON_LENGTH - 2] = seed[1] + 128;
      cells[AUTOMATON_LENGTH - 3] = seed[2] + 128;
      cells[AUTOMATON_LENGTH - 4] = seed[3] + 128;

      int seedAsInt = (BITWISE_BYTE_TO_INT & seed[0 + 3])
          | ((BITWISE_BYTE_TO_INT & seed[0 + 2]) << 8)
          | ((BITWISE_BYTE_TO_INT & seed[0 + 1]) << 16)
          | ((BITWISE_BYTE_TO_INT & seed[0]) << 24);
      if (seedAsInt != 0xFFFFFFFF) {
        seedAsInt++;
      }
      for (int i = 0; i < AUTOMATON_LENGTH - 4; i++) {
        cells[i] = 0x000000FF & (seedAsInt >> (i % 32));
      }

      // Evolve automaton before returning integers.
      for (int i = 0; i < AUTOMATON_LENGTH * AUTOMATON_LENGTH / 4; i++) {
        internalNext(32);
      }
    } finally {
      lock.unlock();
    }
  }

  @EnsuresNonNull("cells")
  @RequiresNonNull({"lock", "seed"})
  private void initSubclassTransientFields(
      @UnknownInitialization(BaseEntropyCountingRandom.class)CellularAutomatonRandom this) {
    cells = new int[AUTOMATON_LENGTH];
    lock.lock();
    try {
      setSeed(seed);
      copySeedToCellsAndPreEvolve();
    } finally {
      lock.unlock();
    }
  }

  @RequiresNonNull("cells")
  private int internalNext(@UnknownInitialization CellularAutomatonRandom this, int bits) {
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int next(int bits) {
    lock.lock();
    try {
      return internalNext(bits);
    } finally {
      recordEntropySpent(bits);
      lock.unlock();
    }
  }

  @SuppressWarnings("NonFinalFieldReferenceInEquals")
  @Override
  public boolean equals(Object o) {
    return o instanceof CellularAutomatonRandom
        && Arrays.equals(seed, ((CellularAutomatonRandom) o).seed);
  }

  @SuppressWarnings("NonFinalFieldReferencedInHashCode")
  @Override
  public int hashCode() {
    return Arrays.hashCode(seed);
  }

  @SuppressWarnings({"contracts.postcondition.not.satisfied",
      "NonSynchronizedMethodOverridesSynchronizedMethod"})
  @Override
  public void setSeed(@UnknownInitialization CellularAutomatonRandom this, long seed) {
    if (lock == null || this.seed == null) {
      // setSeed can't work until seed array and lock are allocated
      return;
    }
    lock.lock();
    try {
      this.seed[0] = (byte) (seed);
      this.seed[1] = (byte) (seed >> 8);
      this.seed[2] = (byte) (seed >> 16);
      this.seed[3] = (byte) (seed >> 24);
      copySeedToCellsAndPreEvolve();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void setSeed(@UnknownInitialization(BaseRandom.class)CellularAutomatonRandom this,
      byte[] seed) {
    if (seed.length != SEED_SIZE_BYTES) {
      throw new IllegalArgumentException("Cellular Automaton RNG requires a 32-bit (4-byte) seed.");
    }
    lock.lock();
    try {
      if (this.seed == null) {
        this.seed = new byte[SEED_SIZE_BYTES];
      }
      System.arraycopy(seed, 0, this.seed, 0, 4);
      copySeedToCellsAndPreEvolve();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public int getNewSeedLength() {
    return SEED_SIZE_BYTES;
  }
}
