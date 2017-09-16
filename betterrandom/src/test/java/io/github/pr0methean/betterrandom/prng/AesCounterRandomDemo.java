package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.seed.RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR;

import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.BinaryUtils;

public class AesCounterRandomDemo {
  public static void main(String[] args) throws SeedException {
    AesCounterRandom random = new AesCounterRandom(RANDOM_DOT_ORG_SEED_GENERATOR);
    RandomSeederThread.getInstance(RANDOM_DOT_ORG_SEED_GENERATOR).add(random);
    byte[] randomBytes = new byte[32];
    for (int i=0; i<20; i++) {
      random.nextBytes(randomBytes);
      System.out.format("Bytes: %s\n", BinaryUtils.convertBytesToHexString(randomBytes));
    }
  }
}
