# More randomness. Less time. Still an instance of java.util.Random.
[![Build Status](https://dev.azure.com/polymorpheus/BetterRandom/_apis/build/status/BetterRandom-CI?branchName=master)](https://dev.azure.com/polymorpheus/BetterRandom/_build/latest?definitionId=2&branchName=java7)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.pr0methean.betterrandom/BetterRandom/badge.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22io.github.pr0methean.betterrandom%22%20AND%20a%3A%22BetterRandom%22)
[![Coverage](https://codecov.io/gh/Pr0methean/BetterRandom/branch/master/graph/badge.svg)](https://codecov.io/gh/Pr0methean/BetterRandom)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/98a727e6ce3340598e9ae7757f3997fc)](https://www.codacy.com/app/Pr0methean/BetterRandom?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Pr0methean/BetterRandom&amp;utm_campaign=Badge_Grade)
[![codebeat badge](https://codebeat.co/badges/4339b354-590c-4871-b441-d694dc5a33ea)](https://codebeat.co/projects/github-com-pr0methean-betterrandom-master)
[![BCH compliance](https://bettercodehub.com/edge/badge/Pr0methean/BetterRandom?branch=master)](https://bettercodehub.com/)
[![Dependabot Status](https://api.dependabot.com/badges/status?host=github&repo=Pr0methean/BetterRandom)](https://dependabot.com)
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2FPr0methean%2FBetterRandom.svg?type=shield)](https://app.fossa.io/projects/git%2Bgithub.com%2FPr0methean%2FBetterRandom?ref=badge_shield)
[![Lifted on Tidelift!](https://tidelift.com/badges/package/maven/io.github.pr0methean.betterrandom:BetterRandom)](https://tidelift.com/subscription/pkg/maven-io-github-pr0methean-betterrandom-betterrandom-java7?utm_source=maven-io-github-pr0methean-betterrandom-betterrandom-java7&utm_medium=referral&utm_campaign=readme)
<a href="https://www.buymeacoffee.com/IMypBXw"><img src="https://bmc-cdn.nyc3.digitaloceanspaces.com/BMC-button-images/custom_images/yellow_img.png?c=1" alt="Buy me a Coffee!" height="20px"></a>

BetterRandom is a library that helps you get the best performance and the best pseudorandomness from
your pseudorandom-number generators (PRNGs) and their seed sources. With it, you can:

* Wrap a `SplittableRandom`, with its improved speed and randomness-test performance, as a
  `java.util.Random` so that methods like
  [Collections.shuffle(List<>,Random)](https://docs.oracle.com/javase/8/docs/api/java/util/Collections.html#shuffle-java.util.List-java.util.Random-)
  can use it.
  * Have it automatically split when accessed from a new thread, so that it's even faster than a
    `ThreadLocalRandom` but still safe to pass between threads.
* Wrap any `Supplier<Random>` to create a `Random` that's thread-local, but can be accessed just
  like any other `Random`.
* Use `AesCounterRandom`, a PRNG that's as secure as AES but much faster than `SecureRandom`.
* Use any of 5 other great PRNG algorithms.
* Use the above PRNGs anywhere you can use `java.util.Random`, because they're *subclasses* of
  `java.util.Random`.
* Be confident that your PRNGs will serialize and deserialize correctly.
* Make the most of a custom PRNG algorithm by extending the abstract class `BaseRandom`.
* Keep track of how much entropy your PRNG has left (bits of output versus bits of seed).
* Automatically reseed your PRNGs whenever it's possible and beneficial, without blocking the
  threads that use them while a seed is generated. The `RandomSeederThread` class does this and can
  use any of the following seed sources:
  * `/dev/random`
  * [random.org](https://random.org)
  * A `SecureRandom`
  * Automatically choose the best of the above three with `DefaultSeedGenerator`.
  * Or roll your own by implementing just one method from the `SeedGenerator` interface.

# But java.util.Random is already fast enough for me!

Yeah, but it's not *random* enough for you. Monte Carlo simulations have been known to
[give misleading results](http://physics.ucsc.edu/~peter/115/randu.pdf) because of low-quality
PRNGs, and the implementation in `java.util.Random` is low-quality for two reasons:

* It's a linear congruential generator, a notoriously bad algorithm.
* It only has 48 bits of internal state, so some `long` and `double` values are impossible to
  generate. This means "uniform distributions" aren't uniform.

Many standard tests of randomness amount to Monte Carlo simulations. And since widespread
pseudorandom number generators (PRNGs) pass most but not all such tests in standard suites such as
BigCrush and Dieharder, this suggests that *any* Monte Carlo simulation may turn out to be a test of
randomness, and to give misleading or untrustworthy results because of an unfortunate choice of
PRNG. There are two ways to minimize this risk, both of which BetterRandom can help with:

* Have several different PRNG algorithms available, all with the same interfaces, so that you can
  swap one out and compare results.
* Reseed PRNGs as often as possible, ideally with a seed source that continues to receive entropy
  in parallel with your simulation.

Don't take chances on randomness -- get it right with BetterRandom.

# Professional support

## io.github.pr0methean.betterrandom:BetterRandom for enterprise

Available as part of the Tidelift Subscription

The maintainers of io.github.pr0methean.betterrandom:BetterRandom and thousands of other packages are working with Tidelift to deliver commercial support and maintenance for the open source dependencies you use to build your applications. Save time, reduce risk, and improve code health, while paying the maintainers of the exact dependencies you use. [Learn more.](https://tidelift.com/subscription/pkg/maven-io-github-pr0methean-betterrandom-betterrandom?utm_source=maven-io-github-pr0methean-betterrandom-betterrandom&utm_medium=referral&utm_campaign=enterprise&utm_term=repo)

# Your name or ad here!

GitHub Sponsors can get mentions or ad space in this section of this page, both on
[GitHub](https://github.com/pr0methean/betterrandom) and
[GitHub Pages](https://pr0methean.github.io/betterrandom-java7/index.html). You can even get a
mention in the Javadocs.
Get publicity from a library that was downloaded 13,854 times from Maven Central in October 2019.
Visit [https://github.com/sponsors/Pr0methean](https://github.com/sponsors/Pr0methean) for details.

# Get it from MavenCentral

* Get the latest version from
[Maven Central](https://search.maven.org/#search%7Cga%7C1%7Ca%3A%22BetterRandom).
* Get dependency codes for Maven, Gradle, SBT, Ivy, Grape, Leiningen and Buildr from
  [mvnrepository.com](https://mvnrepository.com/artifact/io.github.pr0methean.betterrandom)
  (more full-featured than Maven Central, but not always up-to-date).

At both links, choose **BetterRandom** if using JDK 8+ and/or Android API 24+ at both compile-time
and runtime. Otherwise, choose **BetterRandom-Java7**.

# Quick start: picking a PRNG

When you need an instance of `java.util.Random`, the recommended BetterRandom solution depends on how you're using it.

## It's for cryptography or gambling

Only `AesCounterRandom` has cryptographically secure output.
Continuous reseeding is recommended if you don't need reproducible output.

* If you need reproducible output, use:
  ```
  byte[] seed;
  // Use an input seed if provided, or else:
  if (seed == null) {
    seed = DEFAULT_SEED_GENERATOR.generateSeed(AesCounterRandom.MAX_SEED_LENGTH_BYTES);
    // Then output the seed
  }
  random = new AesCounterRandom(seed);
  ```
* If you need multi-thread concurrency (incompatible with reproducibility), use:
  ```
  new ReseedingThreadLocalRandomWrapper(AesCounterRandom::new, new RandomSeederThread(DEFAULT_SEED_GENERATOR))
  ```
* Otherwise, use:
  ```
  random = new AesCounterRandom();
  new RandomSeederThread(DEFAULT_SEED_GENERATOR).add(random);
  ```

## It's for a scientific simulation, competitive multiplayer game, or professional creative work

For these purposes, the SplittableRandomAdapter family is fastest and passes the Dieharder pseudo-randomness tests.
Continuous reseeding is recommended if you don't need reproducible output.

* If you need reproducible output, use:
  ```
  byte[] seed;
  // Get an input seed if provided, then:
  if (seed == null) {
    seed = DEFAULT_SEED_GENERATOR.generateSeed(Long.BYTES);
    // Then output the seed
  }
  random = new SingleThreadSplittableRandomAdapter(seed); 
  ```
  See `FifoFiller` for an example that reads the seed from an environment variable.
* If you need multi-thread concurrency (incompatible with reproducibility), use:
  ```
  new ReseedingSplittableRandomAdapter()
  ```
* Otherwise, use:
  ```
  random = new SingleThreadSplittableRandomAdapter();
  new RandomSeederThread(DEFAULT_SEED_GENERATOR).add(random);  
  ```

## It's for a single-player game, screensaver, playlist shuffle, etc.

* If you need multi-thread concurrency *and* to be able to use a specific seed on each thread, use:
  ```
  new SplittableRandomAdapter()
  ```
* If you need to be able to use a specific seed, but you don't need concurrency, use:
  ```
  new SingleThreadSplittableRandomAdapter()
  ```
* Otherwise, use:
  ```
  ThreadLocalRandom.current()
  ```
  (On OpenJDK 8 and Android API 24+, this is as good as a SplittableRandom.)

# Simple tricks

## Disable random.org unless explicitly specified
```
// Run this before creating any PRNG instances.
DefaultSeedGenerator.set(new SeedGeneratorPreferenceList(
      new BufferedSeedGenerator(DevRandomSeedGenerator.DEV_RANDOM_SEED_GENERATOR, 128),
      SecureRandomSeedGenerator.DEFAULT_INSTANCE));
```

## Specify the SecureRandom algorithm for seed generation
```
SecureRandom secureRandom = new SecureRandom("SHA1PRNG");
DefaultSeedGenerator.set(new SecureRandomSeedGenerator(secureRandom));
```

## Use a different seed generator for one specific PRNG
```
random = new SingleThreadSplittableRandomAdapter(SecureRandomSeedGenerator.DEFAULT_INSTANCE);
```

## Ridiculously long period
```
random = new Cmwc4096Random();
```


# Full examples

## Cryptographic PRNG that uses Random.org for frequent reseeding
```java
import static io.github.pr0methean.betterrandom.seed.RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR;

import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.BinaryUtils;

public class AesCounterRandomDemo {
  public static void main(String[] args) throws SeedException {
    AesCounterRandom random = new AesCounterRandom(RANDOM_DOT_ORG_SEED_GENERATOR);
    RandomSeederThread.add(SECURE_RANDOM_SEED_GENERATOR, random);
    byte[] randomBytes = new byte[32];
    for (int i=0; i<20; i++) {
      random.nextBytes(randomBytes);
      System.out.format("Bytes: %s\n", BinaryUtils.convertBytesToHexString(randomBytes));
    }
  }
}
```

## ReseedingSplittableRandomAdapter for fast, high-quality, parallel duplicate-bridge dealing
```java
import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SplittableRandomAdapterDemo {
  private static final String[] VALUE_LABELS = {"A","K","Q","J","10","9","8","7","6","5","4","3","2"};
  private static final String[] SUIT_LABELS = {"♥️","♣️","♦️","♠️"};
  public static void main(String[] args) throws SeedException, InterruptedException {
    String[] cards = new String[52];
    int i=0;
    for (String suit : SUIT_LABELS) {
      for (String value : VALUE_LABELS) {
        cards[i] = value + suit;
        i++;
      }
    }
    ThreadLocal<List<String>> deckCopies = ThreadLocal.withInitial(() -> Arrays.asList(cards.clone()));
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4);
    ReseedingSplittableRandomAdapter random =
        new ReseedingSplittableRandomAdapter(SecureRandomSeedGenerator.DEFAULT_INSTANCE);
    for (i=0; i<1000; i++) {
      executor.submit(() -> {
        List<String> deck = deckCopies.get();
        Collections.shuffle(deck, random);
        System.out.format("North: %s%nEast: %s%nSouth: %s%nWest: %s%n%n",
            String.join(",", deck.subList(0, 13)),
            String.join(",", deck.subList(13, 26)),
            String.join(",", deck.subList(26, 39)),
            String.join(",", deck.subList(39, 52)));
      });
    }
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.MINUTES);
  }
}

```
# Full javadocs

Javadocs for the latest snapshot, including both public and protected members (to support your
subclassing), are at [pr0methean.github.io](https://pr0methean.github.io/).

# Tested environments and supported versions

BetterRandom has 2 branches from which releases are cut: one for Java 7 -- including Android API
levels below 24 -- and one for Java 8 and newer. These produce the Maven artifacts named
`BetterRandom-Java7` and `BetterRandom` respectively.

## Java 8 (master branch)

Beginning with version 3.1.2, continuous integration has been moved to Azure Pipelines and includes
the following environments:

* Ubuntu: OpenJDK 8,11,12; OpenJ9 8 and 12; Oracle HotSpot 8 and 11
* OSX: OpenJDK 8,11; Oracle HotSpot 11
* Windows: OpenJDK 8,11,12

OpenJDK 8, 11 and 12 on Ubuntu and Windows are Azul Zulu builds. OpenJ9 is AdoptOpenJDK.
For OpenJDK on OSX, the preinstalled builds on Microsoft's OSX system images are used;
their provenance is unclear, but they are to be replaced with Azul Zulu in April 2019.

For versions up to 3.1.1, continuous integration took place in the following environments:

* Linux (on Travis): OpenJDK and Oracle JDK 8 and up
* OS X (on Travis): OpenJDK 8 and up, Xcode 7.3 and up
* Windows Server 2008 R2 (on Appveyor): Oracle JDK 8
* Cygwin (on Appveyor): Oracle JDK 8
* MinGW (on Appveyor): Oracle JDK 8

CI on BSD or Android isn't likely any time soon, since no free providers of BSD CI seem to be
integrated with GitHub, and there seems to be no actively-maintained Android-app wrapper for TestNG
suites. However, Android API levels 24 and up (required as both source and target) should work.

## Java 7 (java7 branch)

This branch is mainly intended to support Android API levels 19 through 23; support for other
environments is best-efforts.

Continuous integration takes place in OpenJDK 7 on Linux. Up to and including version 3.1.1, this was
done on Travis CI; beginning with 3.1.2, Azure Pipelines will be used.

After version 4.4.0, the Java 7 branch will only be maintained while at least one
Tidelift subscriber, or Gold or higher GitHub sponsor, is using it.

# Alternative random number generators

BetterRandom provides several pseudorandom number generators that are intended as drop-in
replacements for `java.util.Random`.

## Features common to all PRNGs in `betterrandom.prng`

* Reproducible: The `getSeed()` function retrieves a seed that can be passed into the constructor to
  create another PRNG giving the same output. If any random numbers have been generated since
  construction or a call to `setSeed(byte[])`, this may rewind the state to before that happened.

* Serializable: All these PRNGs can be serialized and deserialized, copying their full internal
  state.

* `setSeed(byte[])`: Reseed even if more than a `long` is needed to do so.
** Use `getNewSeedLength()` to get the recommended seed size.

* `entropyBits()`: Find out when the PRNG has output more random data than it has been seeded with,
  and thus could benefit from being reseeded. Even when the PRNG is reseeded repeatedly without
  being used, the entropy count won't ever go above the size of the PRNG's internal state.

* `withProbability(double)`: Get a boolean with *any* probability of being true, not just 50/50, and
  the PRNG will still know you're only spending 1 bit of entropy. (Technically it may be [*less*
  than 1 bit](https://en.wikipedia.org/wiki/Binary_entropy_function), but we don't implement
  fractional-bit counting yet.)

* Lockless `nextGaussian()`.

* `doubles`, `ints` and `longs` will return parallel streams whenever possible, whereas their
  implementations in `Random` always return sequential streams.

* `gaussians()` and `gaussians(long streamSize)`: Get an endless or finite stream of
  normally-distributed doubles.

* `setSeederThread(RandomSeederThread)`: Reseeds the PRNG whenever its entropy is spent, but only
  as long as a seed generator can keep up. See below.

## SplittableRandom adapters

These classes use `java8.util.SplittableRandom` instances to implement the methods of `Random`,
despite that the two classes are unrelated and have slightly different method signatures. Several
adapters are available:

* `SingleThreadSplittableRandomAdapter`: Simple and fast, but not thread-safe.
* `SplittableRandomAdapter`: Backed by a `ThreadLocal<SplittableRandom>`, whose instances of
  `SplittableRandom` are all split from a single master.
* `ReseedingSplittableRandomAdapter`: Also backed by a `ThreadLocal<SplittableRandom>`, this
  registers each thread's `SplittableRandom` instance with a `RandomSeederThread` (see below). This
  is probably the best PRNG implementation that allows concurrent access from multiple threads.

## Table of algorithms

| Class                   | Seed size (bytes)  | Period (bits)      |  Speed | Speed with RandomSeederThread | Effect of `setSeed(long)`                     | `getSeed()` rewinds? | Algorithm author
|-------------------------|--------------------|--------------------|--------|-------------------------------|-----------------------------------------------|----------------------|--------------------
| AesCounterRandom        |  16-48<sup>*</sup> | 2<sup>135</sup>    |   Slow |                          Slow | Combines with existing seed                   | No                   | [NIST](http://csrc.nist.gov/groups/ST/toolkit/documents/rng/BlockCipherDRBGs.pdf)
| Cmwc4096Random          |              16384 | 2<sup>131104</sup> | Medium |                     Very slow | Not supported                                 | Yes                  | [George Marsaglia](http://school.anhb.uwa.edu.au/personalpages/kwessen/shared/Marsaglia03.html)
| MersenneTwisterRandom   |                 16 | 2<sup>19937</sup>  | Medium |                        Medium | Not supported                                 | Yes                  | [Makoto Matsumoto](http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html)
| XorShiftRandom          |                 20 | ~2<sup>160</sup>   | Medium |                        Medium | Not supported                                 | Yes                  | [George Marsaglia](http://www.jstatsoft.org/v08/i14/paper)
| SplittableRandomAdapter |     8<sup>**</sup> | 2<sup>64</sup>     |   Fast |              Fast<sup>†</sup> | Replaces existing seed (calling thread only)  | Yes                  | [Guy Steele and Doug Lea](http://hg.openjdk.java.net/jdk8/jdk8/jdk/file/687fd7c7986d/src/share/classes/java/util/SplittableRandom.java)
| Pcg64Random             |                  8 | 2<sup>62</sup>     |   Fast |                          Fast | Replaces existing seed                        | Yes                  | [M. E. O'Neill](http://www.pcg-random.org/)

Period assumes exactly 32 or 64 bits are consumed at a time, using `nextInt()`, `nextLong()`, `ints()` or `longs()`.

<sup>*</sup>Seed sizes above 32 for AesCounterRandom require jurisdiction policy files that allow
192- and 256-bit AES seeds.

<sup>**</sup>Can be reseeded independently on each thread, affecting only that thread.

<sup>†</sup>Use specialized subclass ReseedingSplittableRandomAdapter.

### AesCounterRandom

Retrieving the internal state of an `AesCounterRandom` instance from its output is considered
equivalent to breaking the AES cipher. Thus, this class should be able to replace `SecureRandom` in
many applications, such as generating session keys or erasing files on a magnetic disk or tape.

AesCounterRandom only generates a *permutation* of the space of 128-bit integers, so if it is used
to generate about 2<sup>64</sup> 128-bit strings without reseeding, its statistical properties will
begin to differ from those of `/dev/random` in that it won't have
[generated the same string twice](https://en.wikipedia.org/wiki/Birthday_problem). This could be
prevented by using a hash function rather than a reversible cipher, but the hash functions in
standard JVMs are less cryptographically secure than AES and won't run as fast on hardware featuring
AES-NI.

# Reseeding

## `SeedGenerator`

A `SeedGenerator` produces seeds for PRNGs. All the provided implementations are singletons, because
the seed sources cannot be parallelized. They include:

* `DevRandomSeedGenerator.DEV_RANDOM_SEED_GENERATOR`: Works only on Unix-like systems; reads seeds
  from `/dev/random`.
* `RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR`: Connects to
  [random.org](https://www.random.org) to retrieve random numbers over HTTPS.
  Random.org collects randomness from atmospheric noise using 9 radios, located at undisclosed
  addresses in Dublin and Copenhagen and tuned to undisclosed AM/FM frequencies. (The secrecy is
  intended to help prevent tampering with the output using a well-placed radio transmitter, and the
  use of AM/FM helps ensure that any such tampering would cause illegal interference with broadcasts
  and quickly attract regulatory attention.) Uses the legacy API by default, but can be configured
  to use the JSON-RPC API.
  * `RandomDotOrgSeedGenerator.RATE_LIMITED_ON_FAIL`: Avoids spamming Random.org or your router, by
    instantly reporting failure for 10 seconds after every I/O or HTTP error.
* `SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR`: Uses
  `java.security.SecureRandom.generateSeed`. On Oracle and OpenJDK, this in turn uses
  `sun.security.provider.SeedGenerator`; when `/dev/random` isn't available, that in turn uses the
  timing of newly-launched threads as a source of randomness, relying on the unpredictable
  interactions between different configurations of hardware and software and their workloads.
* `DefaultSeedGenerator.DEFAULT_SEED_GENERATOR`: Uses the best of the above three that is currently
  available.

## SimpleRandomSeeder

This is a daemon thread that loops over all the `ByteArrayReseedableRandom` instances registered
with it and reseeds them. Those that implement `EntropyCountingRandom` are skipped when they still\
have entropy left from a previous seeding. Example usage:

```
// Obtain the seeder thread for this seed generator; launch it if it's not already running.
RandomSeederThread seederThread = RandomSeederThread.getInstance(DEFAULT_SEED_GENERATOR);

// Thread is now running, but is asleep if no PRNGs have already been added to it.

// Begin including myRandom in the loop, and wake up the thread.
seederThread.add(myRandom);

// Manually trigger reseeding ASAP (myRandom must be added first)
if (myRandom instanceof EntropyCountingRandom) {
  seederThread.asyncReseed(myRandom);
}

// Adding the same PRNG a second time has no effect
seederThread.add(myRandom);
```

## LegacyRandomSeeder

This class is similar to `SimpleRandomSeeder`, but can handle any instance of `java.util.Random`.

# Folders

* `betterrandom/` contains BetterRandom's source code.
* `benchmark/` defines the JMH benchmarks as a separate Maven project.
* `FifoFiller/` is a simple utility used for Dieharder randomness tests, and is also a separate
  Maven project.
* `etc/` contains scripts, templates used in `azure.yml`, and miscellaneous files used for
  building and testing.
* `docs/` is a Git submodule used for publishing updates to this project's
  [Javadoc on github.io](https://pr0methean.github.io/betterrandom-java8/index.html).

# Credits

The following classes are forked from [Uncommons Maths](https://github.com/dwdyer/uncommons-maths/):

* All of `betterrandom.prng` except `BaseRandom` and `betterrandom.prng.adapter`
* All of `betterrandom.seed` except `RandomSeederThread`
* `BinaryUtils`
* Test classes corresponding to the above.


## License
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2FPr0methean%2FBetterRandom.svg?type=large)](https://app.fossa.io/projects/git%2Bgithub.com%2FPr0methean%2FBetterRandom?ref=badge_large)
