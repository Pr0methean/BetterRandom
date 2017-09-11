[![Build Status](https://travis-ci.org/Pr0methean/BetterRandom.svg?branch=master)](https://travis-ci.org/Pr0methean/BetterRandom)
[![Coverage Status](https://coveralls.io/repos/github/Pr0methean/BetterRandom/badge.svg?branch=master)](https://coveralls.io/github/Pr0methean/BetterRandom?branch=master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/98a727e6ce3340598e9ae7757f3997fc)](https://www.codacy.com/app/Pr0methean/BetterRandom?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Pr0methean/BetterRandom&amp;utm_campaign=Badge_Grade)

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
  and thus could benefit from being reseeded. Even when reseeded repeatedly without being used, the
  entropy count won't ever go above the size of the PRNG's internal state.

* `setSeederThread(RandomSeederThread)`: Reseeds the PRNG whenever its entropy is spent, but only
  as long as a seed generator can keep up. See below.

## Summary

| Class                   | Seed size (bytes)  | Period (bits)      |  Speed | Speed with RandomSeederThread | Effect of `setSeed(long)`                     | `getSeed()` rewinds? | Algorithm author
|-------------------------|--------------------|--------------------|--------|-------------------------------|-----------------------------------------------|----------------------|--------------------
| AesCounterRandom        |  16-48<sup>*</sup> | 2<sup>135</sup>    |   Slow |                          Slow | Combines with existing seed                   | No                   | [NIST](http://csrc.nist.gov/groups/ST/toolkit/documents/rng/BlockCipherDRBGs.pdf)
| CellularAutomatonRandom |                  4 | ?                  | Medium |                     Very slow | Replaces existing seed                        | Yes                  | [Anthony Pasqualoni](http://web.archive.org/web/20160413212616/http://home.southernct.edu/~pasqualonia1/ca/report.html)
| Cmwc4096Random          |              16384 | 2<sup>131104</sup> | Medium |                     Very slow | Not supported                                 | Yes                  | [George Marsaglia](http://school.anhb.uwa.edu.au/personalpages/kwessen/shared/Marsaglia03.html)
| MersenneTwisterRandom   |                 16 | 2<sup>19937</sup>  | Medium |                        Medium | Not supported                                 | Yes                  | [Makoto Matsumoto](http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html)
| XorShiftRandom          |                 20 | ~2<sup>160</sup>   | Medium |                        Medium | Not supported                                 | Yes                  | [George Marsaglia](http://www.jstatsoft.org/v08/i14/paper)
| SplittableRandomAdapter |                  8 | 2<sup>64</sup>     |   Fast |              Fast<sup>†</sup> | Replaces existing seed (calling thread only)  | Yes                  | [Guy Steele and Doug Lea](http://hg.openjdk.java.net/jdk8/jdk8/jdk/file/687fd7c7986d/src/share/classes/java/util/SplittableRandom.java)

<sup>*</sup>Seed sizes above 32 for AesCounterRandom require jurisdiction policy files that allow
192- and 256-bit AES seeds.
<sup>**</sup>Can be reseeded independently on each thread, affecting only that thread.
<sup>†</sup>Use specialized subclass ReseedingSplittableRandomAdapter.

## AesCounterRandom

Retrieving the internal state of an `AesCounterRandom` instance from its output is considered
equivalent to breaking the AES cipher.

AesCounterRandom only generates a *permutation* of the space of 128-bit integers, so if it is used
to generate about 2<sup>64</sup> 128-bit strings without reseeding, its statistical properties will
begin to differ from those of `/dev/random` in that it won't have
[generated the same string twice](https://en.wikipedia.org/wiki/Birthday_problem). This could be
prevented by using a hash function rather than a reversible cipher, but the hash functions in
standard JVMs are less cryptographically secure than AES and won't run as fast on hardware featuring
AES-NI.

# SplittableRandom adapters

These classes use `java.util.SplittableRandom` instances to implement the methods of `Random`,
despite that the two classes are unrelated and have slightly different method signatures. Several
adapters are available:

* `SingleThreadSplittableRandomAdapter`: Simple and fast, but not thread-safe.
* `SplittableRandomAdapter`: Backed by a `ThreadLocal<SplittableRandom>`, whose instances of
  `SplittableRandom` are all split from a single master.
* `ReseedingSplittableRandomAdapter`: Also backed by a `ThreadLocal<SplittableRandom>`, this
  registers each thread's `SplittableRandom` instance with a `RandomSeederThread` (see below). This
  is probably the best PRNG implementation that allows concurrent access from multiple threads.

# Reseeding

## `SeedGenerator`

A `SeedGenerator` produces seeds for PRNGs. All the provided implementations are singletons, because
the seed sources cannot be parallelized. They include:

* `DevRandomSeedGenerator.DEV_RANDOM_SEED_GENERATOR`: Works only on Unix-like systems; reads seeds
  from `/dev/random`.
* `RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR`: Uses the
  [random.org old API](https://www.random.org/clients/http/) to retrieve random numbers over HTTPS.
  Random.org collects randomness from atmospheric noise using 9 radios, located at undisclosed
  addresses in Dublin and Copenhagen and tuned to undisclosed AM/FM frequencies. (The secrecy is
  intended to help prevent tampering with the output using a well-placed radio transmitter, and the
  use of AM/FM helps ensure that any such tampering would cause illegal interference with broadcasts
  and quickly attract regulatory attention.) Note that random.org limits the supply of free random
  numbers to any one IP address; if you operate from a fixed address (at least if you use IPv4), you
  can [check your quota and buy more](https://www.random.org/quota/).
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

* `benchmark.sh`: Compile and run benchmarks. Output will be in `benchmark/target`.
* `benchmark-extra-metrics.sh`: Compile and run the same benchmarks; but in addition to throughput,
  also collect many extra metrics concerning garbage collection, lock/monitor performance, and
  thread births and deaths. May require HotSpot JVM.
* `coverage.sh`: Compile and run tests and generate coverage reports. Upload them to Coveralls if
  running in Travis-CI.
* `release.sh`: Used to perform new releases.
* `test-proguard.sh`: Compile and run tests against Proguarded jar.
* `prepare-workspace.sh`: Install necessary packages on a fresh Ubuntu Trusty Tahr workspace, such
  as what c9.io provides.

# Credits

The following classes are forked from [Uncommons Maths](https://github.com/dwdyer/uncommons-maths/):

* All of `betterrandom.prng` except `BaseRandom` and `betterrandom.prng.adapter`
* All of `betterrandom.seed` except `RandomSeederThread`
* `BinaryUtils`
* `BitString`
* Test classes corresponding to the above.
