package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Temporary workaround for https://github.com/cbeust/testng/issues/1563
 * FIXME: Unsuccessful! Update once someone answers https://github.com/cbeust/testng/issues/1563
 */
public class AesCounterTestNgBug1563WorkaroundTest extends AesCounterRandom128Test {

  @Override
  public void testSetSeed() throws SeedException {
    super.testSetSeed();
  }

  @Override
  public void testReseeding() throws Exception {
    super.testReseeding();
  }

  @Override
  public void testMaxSeedLengthOk() {
    super.testMaxSeedLengthOk();
  }

  @Override
  protected BaseRandom tryCreateRng() throws SeedException {
    return super.tryCreateRng();
  }

  @Override
  protected BaseRandom createRng(byte[] seed) throws SeedException {
    return super.createRng(seed);
  }

  @Override
  public void testRepeatability() throws SeedException {
    super.testRepeatability();
  }

  @Override
  public void testSeedTooLong() throws GeneralSecurityException, SeedException {
    super.testSeedTooLong();
  }

  @Override
  public void testDistribution() throws SeedException {
    super.testDistribution();
  }

  @Override
  public void testStandardDeviation() throws SeedException {
    super.testStandardDeviation();
  }

  @Override
  public void testSeedTooShort() throws SeedException {
    super.testSeedTooShort();
  }

  @Override
  public void testNullSeed() throws SeedException {
    super.testNullSeed();
  }

  @Override
  public void testSerializable() throws IOException, ClassNotFoundException, SeedException {
    super.testSerializable();
  }

  @Override
  public void testEquals() throws SeedException {
    super.testEquals();
  }

  @Override
  public void testHashCode() throws Exception {
    super.testHashCode();
  }

  @Override
  public void testDump() throws SeedException {
    super.testDump();
  }

  @Override
  protected BaseRandom createRng() {
    return super.createRng();
  }

  @Override
  public void testWithProbability() {
    super.testWithProbability();
  }

  @Override
  public void testNextBytes() throws Exception {
    super.testNextBytes();
  }

  @Override
  public void testNextInt1() throws Exception {
    super.testNextInt1();
  }

  @Override
  public void testNextInt2() throws Exception {
    super.testNextInt2();
  }

  @Override
  public void testNextLong() throws Exception {
    super.testNextLong();
  }

  @Override
  public void testNextDouble() throws Exception {
    super.testNextDouble();
  }

  @Override
  public void testNextGaussian() throws Exception {
    super.testNextGaussian();
  }

  @Override
  public void testNextBoolean() throws Exception {
    super.testNextBoolean();
  }

  @Override
  public void testInts() throws Exception {
    super.testInts();
  }

  @Override
  public void testInts1() throws Exception {
    super.testInts1();
  }

  @Override
  public void testInts2() throws Exception {
    super.testInts2();
  }

  @Override
  public void testInts3() throws Exception {
    super.testInts3();
  }

  @Override
  public void testLongs() throws Exception {
    super.testLongs();
  }

  @Override
  public void testLongs1() throws Exception {
    super.testLongs1();
  }

  @Override
  public void testLongs2() throws Exception {
    super.testLongs2();
  }

  @Override
  public void testLongs3() throws Exception {
    super.testLongs3();
  }

  @Override
  public void testDoubles() throws Exception {
    super.testDoubles();
  }

  @Override
  public void testDoubles1() throws Exception {
    super.testDoubles1();
  }

  @Override
  public void testDoubles2() throws Exception {
    super.testDoubles2();
  }

  @Override
  public void testDoubles3() throws Exception {
    super.testDoubles3();
  }

  @Override
  public void testNextElementArray() {
    super.testNextElementArray();
  }

  @Override
  public void testNextElementList() {
    super.testNextElementList();
  }

  @Override
  public void testNextEnum() {
    super.testNextEnum();
  }
}
