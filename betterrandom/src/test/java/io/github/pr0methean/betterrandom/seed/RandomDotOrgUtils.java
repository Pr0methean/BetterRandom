package io.github.pr0methean.betterrandom.seed;

import java.util.UUID;

/**
 * Utility methods used in {@link RandomDotOrgSeedGeneratorHermeticTest} and
 * {@link RandomDotOrgSeedGeneratorLiveTest}.
 */
public enum RandomDotOrgUtils {
  ;

  public static boolean haveApiKey() {
    return System.getenv("RANDOM_DOT_ORG_KEY") != null;
  }

  public static void setApiKey() {
    final String apiKeyString = System.getenv("RANDOM_DOT_ORG_KEY");
    RandomDotOrgSeedGenerator
        .setApiKey((apiKeyString == null) ? null : UUID.fromString(apiKeyString));
  }

}
