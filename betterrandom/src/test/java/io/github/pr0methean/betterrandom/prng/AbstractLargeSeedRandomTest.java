package io.github.pr0methean.betterrandom.prng;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.when;

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.lang.reflect.InvocationTargetException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

/**
 * A subclass of {@link BaseRandomTest} for when avoiding
 * {@link io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator}
 * calls is worth the overhead of using PowerMock.
 */
@PrepareForTest(DefaultSeedGenerator.class)
@PowerMockIgnore({"javax.crypto.*", "javax.management.*", "javax.script.*", "jdk.nashorn.*", "javax.net.ssl.*",
    "javax.security.*", "javax.xml.*", "org.xml.sax.*", "org.w3c.dom.*", "org.springframework.context.*", "org.apache.log4j.*",
    "com.sun.*"})
public abstract class
AbstractLargeSeedRandomTest extends BaseRandomTest {

  private DefaultSeedGenerator oldDefaultSeedGenerator;

  /**
   * Replaces the default seed generator with a faster mock. Must be undone after the test using
   * {@link #unmockDefaultSeedGenerator()}.
   */
  protected void mockDefaultSeedGenerator() {
    oldDefaultSeedGenerator = DefaultSeedGenerator.DEFAULT_SEED_GENERATOR;
    final DefaultSeedGenerator mockDefaultSeedGenerator = PowerMockito.mock(DefaultSeedGenerator.class);
    when(mockDefaultSeedGenerator.generateSeed(anyInt())).thenAnswer(new Answer<byte[]>() {
      @Override public byte[] answer(InvocationOnMock invocation) throws Throwable {
        return semiFakeSeedGenerator.generateSeed((Integer) (invocation.getArgument(0)));
      }
    });
    doAnswer(new Answer<Void>() {
      @Override public Void answer(InvocationOnMock invocation) throws Throwable {
        semiFakeSeedGenerator.generateSeed((byte[]) invocation.getArgument(0));
        return null;
      }
    }).when(mockDefaultSeedGenerator).generateSeed(any(byte[].class));
    Whitebox.setInternalState(DefaultSeedGenerator.class, "DEFAULT_SEED_GENERATOR",
        mockDefaultSeedGenerator);
  }

  /**
   * Undoes {@link #mockDefaultSeedGenerator()}, restoring the factory default.
   */
  protected void unmockDefaultSeedGenerator() {
    Whitebox.setInternalState(DefaultSeedGenerator.class, "DEFAULT_SEED_GENERATOR",
        oldDefaultSeedGenerator);
  }

  @Override public void testAllPublicConstructors()
      throws SeedException, IllegalAccessException, InstantiationException,
      InvocationTargetException {
    mockDefaultSeedGenerator();
    try {
      super.testAllPublicConstructors();
    } finally {
      unmockDefaultSeedGenerator();
    }
  }

  @Override public void testSetSeedLong() {
    mockDefaultSeedGenerator();
    try {
      super.testSetSeedLong();
    } finally {
      unmockDefaultSeedGenerator();
    }
  }
}
