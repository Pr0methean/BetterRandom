package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.testng.Assert.assertEquals;

import com.google.common.testing.SerializableTester;
import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import org.mockito.Mockito;

public class ReseedingSplittableRandomAdapterNullTest extends ReseedingSplittableRandomAdapterTest {
  @Override protected ReseedingSplittableRandomAdapter createRng() throws SeedException {
    return new ReseedingSplittableRandomAdapter(getTestSeedGenerator(), null);
  }

  @Override public void testSerializable() throws SeedException {
    final BaseSplittableRandomAdapter adapter =
        new ReseedingSplittableRandomAdapter(DEFAULT_INSTANCE, null);
    final BaseSplittableRandomAdapter clone = SerializableTester.reserialize(adapter);
    assertEquals(adapter, clone, "Unequal after serialization round-trip");
  }

  @Override public void testReseeding() {
    SeedGenerator generator = Mockito.spy(SecureRandomSeedGenerator.DEFAULT_INSTANCE);
    ReseedingSplittableRandomAdapter random = new
        ReseedingSplittableRandomAdapter(generator, null);
    random.nextBytes(new byte[128]);
    Mockito.verify(generator, Mockito.times(1)).generateSeed(anyInt());
    Mockito.verify(generator, Mockito.never()).generateSeed(any(byte[].class));
  }

  @Override public void testSetSeedGeneratorNoOp() {
    createRng().setRandomSeeder(null);
  }
}
