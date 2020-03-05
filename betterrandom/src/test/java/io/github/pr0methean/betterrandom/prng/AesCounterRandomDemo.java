package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;

import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.RandomSeeder;
import io.github.pr0methean.betterrandom.util.BinaryUtils;

public enum AesCounterRandomDemo {
  ;

  public static void main(final String[] args) throws SeedException {
    final AesCounterRandom random = new AesCounterRandom(DEFAULT_INSTANCE);
    new RandomSeeder(DEFAULT_INSTANCE).add(random);
    final byte[] randomBytes = new byte[32];
    for (int i = 0; i < 20; i++) {
      random.nextBytes(randomBytes);
      System.out.format("Bytes: %s%n", BinaryUtils.convertBytesToHexString(randomBytes));
    }
  }
}
