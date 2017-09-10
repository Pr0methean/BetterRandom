#!/bin/sh
cd betterrandom
# Coverage test
mvn jacoco:prepare-agent test jacoco:report -e
STATUS=$?
if [ "$STATUS" = 0 ] && [ "$TRAVIS" = "true" ]; then
  # Post-Proguard test (verifies Proguard settings)
  mvn coveralls:report proguard:proguard test
  STATUS=$?
fi
cd ..
exit "$STATUS"