package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.serializeAndDeserialize;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.testEquivalence;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandomTest;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

public class SingleThreadSplittableRandomAdapterTest extends BaseRandomTest {

  /**
   * Overridden in subclasses, so that subclassing the test can test the subclasses.
   */
  protected BaseSplittableRandomAdapter tryCreateRng() throws SeedException {
    return new SingleThreadSplittableRandomAdapter(
        DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  @Override
  protected BaseRandom createRng(final byte[] seed) throws SeedException {
    final BaseSplittableRandomAdapter adapter = tryCreateRng();
    adapter.setSeed(seed);
    return adapter;
  }

  @Test
  public void testGetSplittableRandom() throws Exception {
    // TODO
  }

  @Override
  @Test
  public void testSerializable() throws SeedException {
    final BaseSplittableRandomAdapter adapter = tryCreateRng();
    // May change when serialized and deserialized, but deserializing twice should yield same object
    // and deserialization should be idempotent
    final BaseSplittableRandomAdapter adapter2 = serializeAndDeserialize(adapter);
    final BaseSplittableRandomAdapter adapter3 = serializeAndDeserialize(adapter);
    final BaseSplittableRandomAdapter adapter4 = serializeAndDeserialize(adapter2);
    testEquivalence(adapter2, adapter3, 20);
    testEquivalence(adapter2, adapter4, 20);
  }

  @Test
  public void testNext() throws Exception {
    // TODO
  }

  @Test
  public void testNextBytes() throws Exception {
    // TODO
  }

  @Test
  public void testNextInt() throws Exception {
    // TODO
  }

  @Test
  public void testNextInt1() throws Exception {
    // TODO
  }

  @Test
  public void testNextInt2() throws Exception {
    // TODO
  }

  @Test
  public void testNextLong() throws Exception {
    // TODO
  }

  @Test
  public void testNextLong1() throws Exception {
    // TODO
  }

  @Test
  public void testNextLong2() throws Exception {
    // TODO
  }

  @Test
  public void testNextDouble() throws Exception {
    // TODO
  }

  @Test
  public void testNextDouble1() throws Exception {
    // TODO
  }

  @Test
  public void testNextDouble2() throws Exception {
    // TODO
  }

  @Test
  public void testNextBoolean() throws Exception {
    // TODO
  }

  @Test
  public void testInts() throws Exception {
    // TODO
  }

  @Test
  public void testInts1() throws Exception {
    // TODO
  }

  @Test
  public void testInts2() throws Exception {
    // TODO
  }

  @Test
  public void testInts3() throws Exception {
    // TODO
  }

  @Test
  public void testLongs() throws Exception {
    // TODO
  }

  @Test
  public void testLongs1() throws Exception {
    // TODO
  }

  @Test
  public void testLongs2() throws Exception {
    // TODO
  }

  @Test
  public void testLongs3() throws Exception {
    // TODO
  }

  @Test
  public void testDoubles() throws Exception {
    // TODO
  }

  @Test
  public void testDoubles1() throws Exception {
    // TODO
  }

  @Test
  public void testDoubles2() throws Exception {
    // TODO
  }

  @Test
  public void testDoubles3() throws Exception {
    // TODO
  }

  @Override
  @Test
  public void testNullSeed() {
    // No-op.
  }

  @Override
  @Test(enabled = false)
  public void testEquals() {
    // No-op.
  }

  @Override
  @Test(enabled = false)
  public void testHashCode() {
    // No-op.
  }
}