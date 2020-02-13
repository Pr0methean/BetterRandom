# `local_policy.jar` and `US_export_policy.jar`

These are the default versions of these files in `$JAVA_HOME/jre/lib/security`
that ship with Oracle jdk1.8.0_131. They allow only 128-bit encryption keys.
are used to test that `AesCounterRandom` will still work given such restrictions.
Since OpenJDK doesn't implement the restrictions, running tests on both
Oracle and OpenJDK covers both branches.

# `scripts/`

* `benchmark.sh`: Compile and run benchmarks. Output will be in `benchmark/target`.
* `unit-tests.sh`: Compile and run unit tests and generate coverage reports. If tests pass, run
  Proguard and then test again (to detect any breakages caused by Proguard).
* `mutation.sh`: Run mutation tests.
* `release.sh`: Used to perform new releases.
* `unrelease.sh`: Used to roll back pom.xml etc. if a release fails.
* `publish-javadoc.sh`: Used to release updated Javadocs to github.io.
* `prepare-workspace.sh`: Install necessary packages on a fresh Ubuntu Trusty Tahr workspace, such
  as what c9.io provides.
