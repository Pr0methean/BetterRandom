package io.github.pr0methean.betterrandom.prng;

import static org.testng.Assert.assertEquals;

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import org.testng.annotations.Test;

public class ThreadLocalRandomWrapperTest extends BaseRandomTest {

  private static BaseRandom createUnderlying() {
    try {
      return new AesCounterRandom();
    } catch (SeedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected Class<? extends BaseRandom> getClassUnderTest() {
    return ThreadLocalRandomWrapper.class;
  }

  @Override
  @Test(enabled = false)
  public void testAllPublicConstructors()
      throws SeedException, IllegalAccessException, InstantiationException, InvocationTargetException {
    // No-op: only 2 ctors, both tested elsewhere.
  }

  @Test
  public void testExplicitSeedSize() throws SeedException {
    assertEquals(new ThreadLocalRandomWrapper(200, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR,
        AesCounterRandom::new).getNewSeedLength(), 200);
  }

  @Test
  public void testWrapLegacy() throws SeedException {
    ThreadLocalRandomWrapper.wrapLegacy(Random::new, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR)
        .nextInt();
  }

  @Override
  protected BaseRandom tryCreateRng() throws SeedException {
    return new ThreadLocalRandomWrapper(ThreadLocalRandomWrapperTest::createUnderlying);
  }

  @Override
  protected BaseRandom createRng(byte[] seed) throws SeedException {
    return tryCreateRng();
  }
}
