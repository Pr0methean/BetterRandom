package io.github.pr0methean.betterrandom.seed;

import java.net.Proxy;
import javax.annotation.Nullable;
import javax.net.ssl.SSLSocketFactory;
import org.testng.annotations.Test;

public class AnuQuantumSeedClientLiveTest extends WebSeedClientLiveTest<AnuQuantumSeedClient> {

  @Test
  public void testGeneratorSmallSeed() {
    SeedTestUtils.testGenerator(seedGenerator, true, 16);
  }

  @Test
  public void testGeneratorLargeRoundSize() {
    SeedTestUtils.testGenerator(seedGenerator, true, 2048);
  }

  @Test
  public void testGeneratorLargeOddSize() {
    SeedTestUtils.testGenerator(seedGenerator, true, 1337);
  }

  @Override protected AnuQuantumSeedClient getSeedGenerator(@Nullable Proxy proxy,
      @Nullable SSLSocketFactory socketFactory) {
    return new AnuQuantumSeedClient(new WebSeedClientConfiguration.Builder()
        .setProxy(proxy)
        .setSocketFactory(socketFactory)
        .build());
  }
}
