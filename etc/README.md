# `local_policy.jar` and `US_export_policy.jar`

These are the default versions of these files in `$JAVA_HOME/jre/lib/security`
that ship with Oracle jdk1.8.0_131. They allow only 128-bit encryption keys.
are used to test that `AesCounterRandom` will still work given such restrictions.
Since OpenJDK doesn't implement the restrictions, running tests on both
Oracle and OpenJDK covers both branches.