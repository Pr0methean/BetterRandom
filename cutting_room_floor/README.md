The contents of this folder are not actively maintained. They're being held here so that if they're
ever needed again, they can be found more easily than with a full history search.

* `benchmark-extra-metrics.sh`: Compile and run the same benchmarks; but in addition to throughput,
  also collect many extra metrics concerning garbage collection, lock/monitor performance, and
  thread births and deaths. May require HotSpot JVM.
* `coverage.sh`: Compile and run unit tests and generate coverage reports. Upload them to Coveralls
  if running in Travis-CI.
* `test-proguard.sh`: Compile and run unit tests against Proguarded jar.
