BetterRandom is a library designed to help improve the quality and performance of random-number
generation on Java. It includes:

* Improved versions of the PRNGs from [Uncommons Maths](https://github.com/dwdyer/uncommons-maths/)
  that support seeding with byte arrays, seed dumping, serialization, and entropy counting.
* The SeedGenerator interface and its implementations from Uncommons Math, with the implementations
  now singletons.
* A RandomSeederThread class that reseeds registered Random instances as frequently as its
  SeedGenerator will allow, but also taking into account the entropy count where available.
* Single-thread and multithread adapter classes that wrap a SplittableRandom as a Random, so that it
  can be used in legacy methods such as
  [Collections.shuffle(List<>,Random)](https://docs.oracle.com/javase/8/docs/api/java/util/Collections.html#shuffle-java.util.List-java.util.Random-)
  that don't have overloads to use with a SplittableRandom. They can be reseeded (this is
  implemented by replacing the SplittableRandom).

[![Build Status (Travis - Linux & OS X)](https://travis-ci.org/Pr0methean/BetterRandom.svg?branch=master)](https://travis-ci.org/Pr0methean/BetterRandom)
[![Build status (Appveyor - Windows)](https://ci.appveyor.com/api/projects/status/fg6siyo4ft98gfff?svg=true)](https://ci.appveyor.com/project/Pr0methean/betterrandom)
[![Coverage Status](https://coveralls.io/repos/github/Pr0methean/BetterRandom/badge.svg?branch=master)](https://coveralls.io/github/Pr0methean/BetterRandom?branch=master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/98a727e6ce3340598e9ae7757f3997fc)](https://www.codacy.com/app/Pr0methean/BetterRandom?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Pr0methean/BetterRandom&amp;utm_campaign=Badge_Grade)
[![codebeat badge](https://codebeat.co/badges/4339b354-590c-4871-b441-d694dc5a33ea)](https://codebeat.co/projects/github-com-pr0methean-betterrandom-master)
[![BCH compliance](https://bettercodehub.com/edge/badge/Pr0methean/BetterRandom?branch=master)](https://bettercodehub.com/)

# Get it from MavenCentral

* Get the latest version from
[Maven Central](https://search.maven.org/#search%7Cga%7C1%7Ca%3A%22BetterRandom).
* Get dependency codes for Maven, Gradle, SBT, Ivy, Grape, Leiningen and Buildr from
  [mvnrepository.com](https://mvnrepository.com/artifact/io.github.pr0methean.betterrandom)
  (more full-featured than Maven Central, but not always up-to-date).

At both links, choose **BetterRandom** if using Java 8+ and Android API 24+ at both compile-time and
runtime. Otherwise, choose **BetterRandom-Java7**.

# Full javadocs

Javadocs for the latest snapshot, including both public and protected members (to support your
subclassing), are at [pr0methean.github.io](https://pr0methean.github.io/).

These Javadocs are for the Java 8+ branch; the Java 7 branch differs mainly in that
`java.util.SplittableRandom`, `java.util.stream.*` and `java.util.function.*` are replaced with
their backported counterparts (whose full names begin with `java8` instead of `java`) in
[StreamSupport](https://sourceforge.net/projects/streamsupport/), which is an extra dependency for
the Java 7 branch.

# Design philosophy: don't take chances on randomness

Many standard tests of randomness amount to Monte Carlo simulations. And since widespread
pseudorandom number generators (PRNGs) pass most but not all such tests in standard suites such as
BigCrush and Dieharder, this suggests that *any* Monte Carlo simulation may turn out to be a test of
randomness, and to give misleading or untrustworthy results because of an unfortunate choice of
PRNG. ([It's happened to scientists before.](http://physics.ucsc.edu/~peter/115/randu.pdf)) There
are two ways to minimize this risk, both of which BetterRandom can help with:

* Have several different PRNG algorithms available, all with the same interfaces.
* Reseed PRNGs as often as possible, ideally with a seed source that continues to receive entropy
  in parallel with your simulation.

# Usage examples

## Cryptographic PRNG that uses Random.org for frequent reseeding
```
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

## ReseedingSplittableRandomAdapter for fast, high-quality parallel bridge dealing
```
import static io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator.DEFAULT_SEED_GENERATOR;

import edu.emory.mathcs.backport.java.util.Collections;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Arrays;
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
    ReseedingSplittableRandomAdapter random = ReseedingSplittableRandomAdapter.getInstance(
        DEFAULT_SEED_GENERATOR);
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
# Supported environments

BetterRandom has 2 versions, one for Java 7 -- including Android API levels below 24 -- and one for 
Java 8 and newer.

## Java 8 (master branch)

Continuous integration takes place in the following environments:

* Linux (on Travis): OpenJDK 8, Oracle JDK 8/9
* OS X (on Travis): JDK 8 (unclear whether Oracle or OpenJDK)
* Cygwin (on Appveyor): JDK 8 (unclear whether Oracle or OpenJDK)
* MinGW (on Appveyor): JDK 8 (unclear whether Oracle or OpenJDK)

CI on BSD or Android isn't likely any time soon, since no free providers of BSD CI seem to be
integrated with GitHub, and there seems to be no actively-maintained Android-app wrapper for TestNG
suites. However, Android API levels 24 and up (required as both source and target) should work.

## Java 7 (java7 branch)

This branch is mainly intended to support Android API levels 19 through 23; support for other
environments is best-efforts. Active development on this branch is likely to end once 80% of Android
devices have API level 24 or newer, as
[measured by Google](https://developer.android.com/about/dashboards/index.html#Platform), *and* Java
18.09 LTS has been released (which will mean Java 7 will be *two* major long-term-support versions
out of date).

Continuous integration takes place in the following environments:

* Linux (on Travis): OpenJDK 7
* Cygwin (on Appveyor): JDK 7 (unclear whether Oracle or OpenJDK)
* MinGW (on Appveyor): JDK 7 (unclear whether Oracle or OpenJDK)

Testing on Oracle JDK 7 is a work in progress.

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

## Other algorithms

| Class                   | Seed size (bytes)  | Period (bits)      |  Speed | Speed with RandomSeederThread | Effect of `setSeed(long)`                     | `getSeed()` rewinds? | Algorithm author
|-------------------------|--------------------|--------------------|--------|-------------------------------|-----------------------------------------------|----------------------|--------------------
| AesCounterRandom        |  16-48<sup>*</sup> | 2<sup>135</sup>    |   Slow |                          Slow | Combines with existing seed                   | No                   | [NIST](http://csrc.nist.gov/groups/ST/toolkit/documents/rng/BlockCipherDRBGs.pdf)
| CellularAutomatonRandom |                  4 | ?                  | Medium |                     Very slow | Replaces existing seed                        | Yes                  | [Anthony Pasqualoni](http://web.archive.org/web/20160413212616/http://home.southernct.edu/~pasqualonia1/ca/report.html)
| Cmwc4096Random          |              16384 | 2<sup>131104</sup> | Medium |                     Very slow | Not supported                                 | Yes                  | [George Marsaglia](http://school.anhb.uwa.edu.au/personalpages/kwessen/shared/Marsaglia03.html)
| MersenneTwisterRandom   |                 16 | 2<sup>19937</sup>  | Medium |                        Medium | Not supported                                 | Yes                  | [Makoto Matsumoto](http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html)
| XorShiftRandom          |                 20 | ~2<sup>160</sup>   | Medium |                        Medium | Not supported                                 | Yes                  | [George Marsaglia](http://www.jstatsoft.org/v08/i14/paper)
| SplittableRandomAdapter |     8<sup>**</sup> | 2<sup>64</sup>     |   Fast |              Fast<sup>†</sup> | Replaces existing seed (calling thread only)  | Yes                  | [Guy Steele and Doug Lea](http://hg.openjdk.java.net/jdk8/jdk8/jdk/file/687fd7c7986d/src/share/classes/java/util/SplittableRandom.java)
| Pcg64Random             |                  8 | 2<sup>62</sup>     |   Fast |                          Fast | Replaces existing seed                        | Yes                  | [M. E. O'Neill](http://www.pcg-random.org/)

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

## RandomSeederThread

This is a daemon thread that loops over all the `Random` instances registered with it and reseeds
them. Those that implement `EntropyCountingRandom` are skipped when they still have entropy left
from a previous seeding. Example usage:

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

# Build scripts

* `benchmark.sh`: Compile and run benchmarks. Output will be in `benchmark/target`. Won't work on
  JDK 9, except on Travis.
* `unit-tests.sh`: Compile and run unit tests and generate coverage reports. Upload them to Coveralls
  if running in Travis-CI. If tests pass, run Proguard and then test again.  Won't work on JDK 9,
  except on Travis.
* `mutation.sh`: Run mutation tests.
* `release.sh`: Used to perform new releases.
* `publish-javadoc.sh`: Used to release updated Javadocs to github.io.
* `prepare-workspace.sh`: Install necessary packages on a fresh Ubuntu Trusty Tahr workspace, such
  as what c9.io provides.

# Credits

The following classes are forked from [Uncommons Maths](https://github.com/dwdyer/uncommons-maths/):

* All of `betterrandom.prng` except `BaseRandom` and `betterrandom.prng.adapter`
* All of `betterrandom.seed` except `RandomSeederThread`
* `BinaryUtils`
* Test classes corresponding to the above.
