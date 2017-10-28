package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.TestUtils.assertGreaterOrEqual;
import static io.github.pr0methean.betterrandom.prng.BaseRandom.ENTROPY_OF_DOUBLE;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.assertMonteCarloPiEstimateSane;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.checkRangeAndEntropy;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.checkStream;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import io.github.pr0methean.betterrandom.TestUtils;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.testng.util.RetryAnalyzerCount;

public abstract class BaseRandomTest {

  /**
   * The square root of 12, rounded from an extended-precision calculation that was done by Wolfram
   * Alpha (and thus at least as accurate as {@code StrictMath.sqrt(12.0)}).
   */
  protected static final double SQRT_12 = 3.4641016151377546;
  protected static final long TEST_SEED = 0x0123456789ABCDEFL;
  private static final int FLAKY_TEST_RETRIES = 3;
  private static final int TEST_BYTE_ARRAY_LENGTH = 20;
  private static final String HELLO = "Hello";
  private static final String HOW_ARE_YOU = "How are you?";
  private static final String GOODBYE = "Goodbye";
  private static final String[] STRING_ARRAY = {HELLO, HOW_ARE_YOU, GOODBYE};
  @SuppressWarnings("StaticCollection") private static final List<String> STRING_LIST =
      Collections.unmodifiableList(Arrays.asList(STRING_ARRAY));
  private static final int ELEMENTS = 100;
  private static final double UPPER_BOUND_FOR_ROUNDING_TEST =
      Double.longBitsToDouble(Double.doubleToLongBits(1.0) + 4);

  protected int getNewSeedLength(final BaseRandom basePrng) {
    return basePrng.getNewSeedLength();
  }

  protected abstract Class<? extends BaseRandom> getClassUnderTest();

  protected abstract BaseRandom createRng();

  protected abstract BaseRandom createRng(byte[] seed);

  @Test public void testReseeding() throws SeedException {
    final byte[] output1 = new byte[20];
    final BaseRandom rng1 = createRng();
    final BaseRandom rng2 = createRng();
    rng1.nextBytes(output1);
    final byte[] output2 = new byte[20];
    rng2.nextBytes(output2);
    final int seedLength = rng1.getNewSeedLength();
    rng1.setSeed(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(seedLength));
    assertGreaterOrEqual(seedLength * 8L, rng1.getEntropyBits());
    rng1.nextBytes(output1);
    rng2.nextBytes(output2);
    assertFalse(Arrays.equals(output1, output2));
  }

  @Test(timeOut = 60000, retryAnalyzer = FlakyTestRetrier.class) public void testRandomSeederThreadIntegration()
      throws Exception {
    final RandomSeederThread seederThread = RandomSeederThread.getInstance(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
    final BaseRandom rng = createRng();
    rng.setSeederThread(seederThread);
    try {
      final byte[] oldSeed = rng.getSeed();
      rng.nextBytes(new byte[oldSeed.length + 1]);
      // wait for 2 iterations if possible
      if (seederThread.awaitIteration(5, TimeUnit.SECONDS)) {
        seederThread.awaitIteration(5, TimeUnit.SECONDS);
      }
      final byte[] newSeed = rng.getSeed();
      assertFalse(Arrays.equals(oldSeed, newSeed));
      assertGreaterOrEqual(newSeed.length * 8L, rng.getEntropyBits());
    } finally {
      rng.setSeederThread(null);
    }
  }

  @AfterClass public void classTearDown() {
    System.gc();
    RandomSeederThread.stopAllEmpty();
  }

  private enum TestEnum {
    RED,
    YELLOW,
    BLUE
  }

  protected static final class FlakyTestRetrier extends RetryAnalyzerCount {

    @EntryPoint public FlakyTestRetrier() {
      setCount(FLAKY_TEST_RETRIES);
    }

    @Override public boolean retryMethod(final ITestResult iTestResult) {
      return !(iTestResult.isSuccess());
    }
  }
}
