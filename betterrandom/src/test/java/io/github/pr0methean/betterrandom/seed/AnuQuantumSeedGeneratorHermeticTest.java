package io.github.pr0methean.betterrandom.seed;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AnuQuantumSeedGeneratorHermeticTest
    extends AbstractWebJsonSeedGeneratorHermeticTest<AnuQuantumSeedGenerator> {

  private static final byte[] RESPONSE_32 = ("{\"type\":\"string\",\"length\":1," +
      "\"size\":32,\"data\":[\"0808446c6d2723c335e3adcf1186c062c1e15c86fb6f396f78ab162b7e28ad33" +
      "\"],\"success\":true}").getBytes(UTF_8);

  @Override protected AnuQuantumSeedGenerator getSeedGeneratorUnderTest() {
    return AnuQuantumSeedGenerator.ANU_QUANTUM_SEED_GENERATOR;
  }


}
