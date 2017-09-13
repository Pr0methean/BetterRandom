#!/bin/sh
if [ "$ANDROID" = 1 ]; then
  MAYBE_ANDROID_FLAG="-Pandroid"
else
  MAYBE_ANDROID_FLAG=""
fi
cd betterrandom
# Coverage test
mvn "$MAYBE_ANDROID_FLAG" clean jacoco:prepare-agent test jacoco:report -e
STATUS=$?
if [ "$STATUS" = 0 ]; then
  if [ "$TRAVIS" = "true" ]; then
    mvn "$MAYBE_ANDROID_FLAG" coveralls:report
  fi
  mvn -DskipTests "$MAYBE_ANDROID_FLAG" package proguard:proguard && (
  rm .surefire-* # Workaround for https://issues.apache.org/jira/browse/SUREFIRE-1414
  # Post-Proguard test (verifies Proguard settings)
  mvn "$MAYBE_ANDROID_FLAG" test -e)
  STATUS=$?
fi
cd ..
exit "$STATUS"