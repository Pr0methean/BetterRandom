package betterrandom.prng.adapter;

import static betterrandom.prng.RandomTestUtils.assertMonteCarloPiEstimateSane;
import static betterrandom.prng.RandomTestUtils.assertStandardDeviationSane;
import static betterrandom.prng.RandomTestUtils.serializeAndDeserialize;
import static betterrandom.prng.RandomTestUtils.testEquivalence;

import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.SeedException;
import org.testng.annotations.Test;

public class SingleThreadSplittableRandomAdapterTest {

  /** Overridden in subclasses, so that subclassing the test can test the subclasses. */
  protected SingleThreadSplittableRandomAdapter createAdapter() throws SeedException {
    return new SingleThreadSplittableRandomAdapter(
        DefaultSeedGenerator.getInstance());
  }

  @Test
  public void testGetSplittableRandom() throws Exception {
    // TODO
  }

  @Test
  public void testSerialization() throws Exception {
    SingleThreadSplittableRandomAdapter adapter = createAdapter();
    // May change when serialized and deserialized, but deserializing twice should yield same object
    // and deserialization should be idempotent
    SingleThreadSplittableRandomAdapter adapter2 = serializeAndDeserialize(adapter);
    SingleThreadSplittableRandomAdapter adapter3 = serializeAndDeserialize(adapter);
    SingleThreadSplittableRandomAdapter adapter4 = serializeAndDeserialize(adapter2);
    testEquivalence(adapter2, adapter3, 20);
    testEquivalence(adapter2, adapter4, 20);
  }

  @Test
  public void testStandardDeviation() throws Exception {
    assertStandardDeviationSane(createAdapter());
  }

  @Test
  public void testMonteCarloPi() throws Exception {
    assertMonteCarloPiEstimateSane(createAdapter());
  }

  @Test
  public void testSetSeed() throws Exception {
    // TODO
  }

  @Test
  public void testSetSeed1() throws Exception {
    // TODO
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

}