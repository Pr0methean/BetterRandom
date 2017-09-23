#!/bin/sh
if [ "$ANDROID" = 1 ]; then
  MAYBE_ANDROID_FLAG="-Pandroid"
else
  MAYBE_ANDROID_FLAG=""
fi
cd betterrandom
# Coverage test
if [ "$TRAVIS_OS_NAME" != "osx" && "$APPVEYOR" = "" ]; then
  mvn $MAYBE_ANDROID_FLAG clean jacoco:prepare-agent test jacoco:report -e
fi
STATUS=$?
if [ "$STATUS" = 0 ]; then
  if [ "$TRAVIS" = "true" ]; then
    mvn $MAYBE_ANDROID_FLAG coveralls:report
  fi
  mvn -DskipTests $MAYBE_ANDROID_FLAG package && (
  # Post-Proguard test (verifies Proguard settings)
  mvn $MAYBE_ANDROID_FLAG test -e)
  STATUS=$?
fi
cd ..
exit "$STATUS"