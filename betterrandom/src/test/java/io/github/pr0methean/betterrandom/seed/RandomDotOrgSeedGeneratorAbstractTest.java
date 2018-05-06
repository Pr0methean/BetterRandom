package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.TestUtils.canRunRandomDotOrgLargeTest;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.UUID;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

public abstract class RandomDotOrgSeedGeneratorAbstractTest extends AbstractSeedGeneratorTest {

  private static final int SMALL_REQUEST_SIZE = 32;
  private static final int TOR_PORT = 9050;
  protected Proxy proxy;

  protected RandomDotOrgSeedGeneratorAbstractTest() {
    super(RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR);
  }

  protected static boolean haveApiKey() {
    return System.getenv("RANDOM_DOT_ORG_KEY") != null;
  }

  protected static void setApiKey() {
    final String apiKeyString = System.getenv("RANDOM_DOT_ORG_KEY");
    RandomDotOrgSeedGenerator
        .setApiKey((apiKeyString == null) ? null : UUID.fromString(apiKeyString));
  }

  @BeforeClass
  public void setUp() {
    proxy = new Proxy(Type.SOCKS, new InetSocketAddress("localhost", TOR_PORT));
    if (!canRunRandomDotOrgLargeTest()) {
      RandomDotOrgSeedGenerator.setMaxRequestSize(SMALL_REQUEST_SIZE);
    }
  }

  @AfterMethod
  public void tearDown() {
    RandomDotOrgSeedGenerator.setApiKey(null);
  }
}
