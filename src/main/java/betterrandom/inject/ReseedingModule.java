package betterrandom.inject;

import betterrandom.prng.adapter.ReseedingSplittableRandomAdapter;
import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.SeedGenerator;
import dagger.Module;
import dagger.Provides;
import java.util.Random;

@Module
public class ReseedingModule {

  @Provides
  public static Random random(ReseedingSplittableRandomAdapter adapter) {
    return adapter;
  }

  @Provides
  public static SeedGenerator seedGenerator() {
    return DefaultSeedGenerator.DEFAULT_SEED_GENERATOR;
  }
}
