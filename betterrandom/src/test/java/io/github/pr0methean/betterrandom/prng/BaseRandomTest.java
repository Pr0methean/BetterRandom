package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.prng.BaseRandom.ENTROPY_OF_DOUBLE;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.assertMonteCarloPiEstimateSane;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.checkRangeAndEntropy;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.checkStream;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import io.github.pr0methean.betterrandom.TestingDeficiency;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.Test;
import org.testng.util.RetryAnalyzerCount;

public abstract class BaseRandomTest {

  @Test
  public void testLongs3() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, 42, prng.longs(20, 1L << 40, 1L << 42), 20, 1L << 40, 1L << 42,
        true);
  }
  
  private enum TestEnum {
    RED,
    YELLOW,
    BLUE;
  }

  /**
   * The square root of 12, rounded from an extended-precision calculation that was done by Wolfram
   * Alpha (and thus at least as accurate as {@code StrictMath.sqrt(12.0)}).
   */
  protected static final double SQRT_12 = 3.4641016151377546;

  private static final LogPreFormatter LOG = new LogPreFormatter(BaseRandomTest.class);
  private static final int MAX_DUMPED_SEED_LENGTH = 32;
  private static final int FLAKY_TEST_RETRIES = 3;
  private static final int TEST_BYTE_ARRAY_LENGTH = 20;

  private static final String HELLO = "Hello";
  private static final String HOW_ARE_YOU = "How are you?";
  private static final String GOODBYE = "Goodbye";
  private static final String[] STRING_ARRAY = {HELLO, HOW_ARE_YOU, GOODBYE};
  private static final List<String> STRING_LIST = Arrays.asList(STRING_ARRAY);

  protected abstract BaseRandom tryCreateRng() throws SeedException;

  protected abstract BaseRandom createRng(byte[] seed) throws SeedException;

  protected BaseRandom createRng() {
    try {
      return tryCreateRng();
    } catch (final SeedException e) {
      throw new RuntimeException(e);
    }
  }

  private static final int ELEMENTS = 100;

  private <E> void testGeneratesAll(Supplier<E> generator, E... expected) {
    final BaseRandom prng = createRng();
    final E[] selected = Arrays.copyOf(expected, ELEMENTS); // Saves passing in a Class<E>
    for (int i = 0; i < ELEMENTS; i++) {
      selected[i] = generator.get();
    }
    assertTrue(Arrays.asList(selected).containsAll(Arrays.asList(expected)));
  }

  private static final class FlakyTestRetrier extends RetryAnalyzerCount {

    @EntryPoint
    public FlakyTestRetrier() {
      setCount(FLAKY_TEST_RETRIES);
    }

    @Override
    public boolean retryMethod(final ITestResult iTestResult) {
      return true;
    }
  }
}
