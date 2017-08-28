package betterrandom.prng;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class BaseRandom extends Random {

  private static final long serialVersionUID = -1556392727255964947L;
  protected static final SecureRandom SECURE_RANDOM = new SecureRandom();

  protected byte[] seed;

  // Lock to prevent concurrent modification of the RNG's internal state.
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  protected transient Lock lock;
  /**
   * Use this to ignore setSeed(long) calls from super constructor
   */
  @SuppressWarnings({"InstanceVariableMayNotBeInitializedByReadObject",
      "FieldAccessedSynchronizedAndUnsynchronized"})
  protected transient boolean superConstructorFinished = false;

  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   */
  public BaseRandom(int seedLength) {
    this(SECURE_RANDOM.generateSeed(seedLength));
  }

  public BaseRandom(byte[] seed) {
    if (seed == null) {
      throw new IllegalArgumentException("Seed must not be null");
    }
    this.seed = seed.clone();
    initTransientFields();
  }

  protected void checkedReadObject(BaseRandom this,
      ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    assert seed != null : "@AssumeAssertion(nullness)";
    initTransientFields();
  }

  @Override
  public synchronized void setSeed(long seed) {
    if (superConstructorFinished) {
      assert lock != null : "@AssumeAssertion(nullness)";
      ByteBuffer buffer = ByteBuffer.allocate(8);
      buffer.putLong(seed);
      setSeed(buffer.array());
    }
  }

  public void setSeed(BaseRandom this, byte[] seed) {
    lock.lock();
    try {
      this.seed = seed.clone();
    } finally {
      lock.unlock();
    }
    assert this.seed != null : "@AssumeAssertion(nullness)";
  }

  private void initTransientFields() {
    if (lock == null) {
      lock = new ReentrantLock();
    }
    superConstructorFinished = true;
  }

  private void readObject(BaseRandom this,
      ObjectInputStream in) throws IOException, ClassNotFoundException {
    checkedReadObject(in);
    initTransientFields();
  }

  private void readObjectNoData() throws InvalidObjectException {
    this.seed = SECURE_RANDOM.generateSeed(getNewSeedLength());
    initTransientFields();
  }

  protected abstract int getNewSeedLength();

  public abstract static class BaseEntropyCountingRandom extends BaseRandom {

    private static final long serialVersionUID = 1838766748070164286L;
    protected final AtomicLong entropyBits = new AtomicLong(0);

    public BaseEntropyCountingRandom(byte[] seed) {
      super(seed);
    }

    @Override
    protected void checkedReadObject(BaseEntropyCountingRandom this,
        ObjectInputStream in)
        throws IOException, ClassNotFoundException {
      super.checkedReadObject(in);
      assert entropyBits != null : "@AssumeAssertion(nullness)";
    }

    @Override
    public void setSeed(BaseEntropyCountingRandom this,
        byte[] seed) {
      assert lock != null : "@AssumeAssertion(nullness)";
      assert entropyBits != null : "@AssumeAssertion(nullness)";
      super.setSeed(seed);
      entropyBits.updateAndGet(oldCount -> Math.max(oldCount, seed.length * 8));
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      super.checkedReadObject(in);
      assert entropyBits != null : "@AssumeAssertion(nullness)";
    }

    protected final void recordEntropySpent(long bits) {
      entropyBits.updateAndGet(oldCount -> Math.max(oldCount - bits, 0));
    }
  }

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
  public static class CellularAutomatonRandom extends BaseEntropyCountingRandom {

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

    private void copySeedToCellsAndPreEvolve(CellularAutomatonRandom this) {
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

    private void initSubclassTransientFields(
        CellularAutomatonRandom this) {
      cells = new int[AUTOMATON_LENGTH];
      lock.lock();
      try {
        setSeed(seed);
        copySeedToCellsAndPreEvolve();
      } finally {
        lock.unlock();
      }
    }

    private int internalNext(CellularAutomatonRandom this, int bits) {
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
    public void setSeed(CellularAutomatonRandom this, long seed) {
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
    public void setSeed(CellularAutomatonRandom this,
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

    public static void test() throws IOException, ClassNotFoundException {
      // Serialise an RNG.
      CellularAutomatonRandom rng = new CellularAutomatonRandom();
      CellularAutomatonRandom result;
      try (
          ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
          ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream)) {
        objectOutStream.writeObject(rng);
        byte[] serialCopy = byteOutStream.toByteArray();
        // Read the object back in.
        try (ObjectInputStream objectInStream = new ObjectInputStream(
            new ByteArrayInputStream(serialCopy))) {
          result = (CellularAutomatonRandom) (objectInStream.readObject());
        }
      }
      System.out.println(result.nextLong());
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
      test();
    }
  }
}
