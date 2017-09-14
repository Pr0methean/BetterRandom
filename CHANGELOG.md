# 0.10.1
* Fixed some bugs involving serialization and deserialization of `LooperThread`.
* Scripts that are not actively maintained now live in the `cutting_room_floor` folder.

# 0.10.0
* Fixed a bug where `BaseRandom` would crash if `setSeed(long)` was called more than once.
* Merged `BaseEntropyCountingRandom` into `BaseRandom`.
* Improvements to test coverage.

# 0.9.2
* Refactorings to increase object reuse, and therefore throughput, in all PRNGs, especially
  `ReseedingSplittableRandomAdapter` (which improved benchmark throughput by a factor of over 300).
* Benchmarks are now available on GitHub.
* `JavaRandom` is now `RandomWrapper`, and can take any Random instance as the one to wrap.
* `RandomWrapper` and `SplittableRandomAdapter` now count entropy.

# 0.9.1.1
* First release on Maven Central.
