package betterrandom.prng.adapter;

import static betterrandom.prng.RandomTestUtils.assertEquivalentWhenSerializedAndDeserialized;
import static betterrandom.prng.RandomTestUtils.serializeAndDeserialize;
import static betterrandom.prng.RandomTestUtils.testEquivalence;

import betterrandom.seed.DefaultSeedGenerator;
import org.testng.annotations.Test;

public class SingleThreadSplittableRandomAdapterTest {

  @Test
  public void testGetSplittableRandom() throws Exception {
  }

  @Test
  public void testSerialization() throws Exception {
    SingleThreadSplittableRandomAdapter adapter = new SingleThreadSplittableRandomAdapter(
        DefaultSeedGenerator.getInstance());
    // May change when serialized and deserialized, but deserializing twice should yield same object
    // and deserialization should be idempotent
    SingleThreadSplittableRandomAdapter adapter2 = serializeAndDeserialize(adapter);
    SingleThreadSplittableRandomAdapter adapter3 = serializeAndDeserialize(adapter);
    SingleThreadSplittableRandomAdapter adapter4 = serializeAndDeserialize(adapter2);
    testEquivalence(adapter2, adapter3, 20);
    testEquivalence(adapter2, adapter4, 20);
  }

  @Test
  public void testSetSeed() throws Exception {
  }

  @Test
  public void testSetSeed1() throws Exception {
  }

  @Test
  public void testNext() throws Exception {
  }

  @Test
  public void testNextBytes() throws Exception {
  }

  @Test
  public void testNextInt() throws Exception {
  }

  @Test
  public void testNextInt1() throws Exception {
  }

  @Test
  public void testNextInt2() throws Exception {
  }

  @Test
  public void testNextLong() throws Exception {
  }

  @Test
  public void testNextLong1() throws Exception {
  }

  @Test
  public void testNextLong2() throws Exception {
  }

  @Test
  public void testNextDouble() throws Exception {
  }

  @Test
  public void testNextDouble1() throws Exception {
  }

  @Test
  public void testNextDouble2() throws Exception {
  }

  @Test
  public void testNextBoolean() throws Exception {
  }

  @Test
  public void testInts() throws Exception {
  }

  @Test
  public void testInts1() throws Exception {
  }

  @Test
  public void testInts2() throws Exception {
  }

  @Test
  public void testInts3() throws Exception {
  }

  @Test
  public void testLongs() throws Exception {
  }

  @Test
  public void testLongs1() throws Exception {
  }

  @Test
  public void testLongs2() throws Exception {
  }

  @Test
  public void testLongs3() throws Exception {
  }

  @Test
  public void testDoubles() throws Exception {
  }

  @Test
  public void testDoubles1() throws Exception {
  }

  @Test
  public void testDoubles2() throws Exception {
  }

  @Test
  public void testDoubles3() throws Exception {
  }

}