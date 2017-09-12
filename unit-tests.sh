#!/bin/sh
cd betterrandom
# Coverage test
mvn jacoco:prepare-agent test jacoco:report -e
STATUS=$?
if [ "$STATUS" = 0 ]; then
  if [ "$TRAVIS" = "true" ]; then
    mvn coveralls:report
  fi
  mvn proguard:proguard
  rm .surefire-* # Workaround for https://issues.apache.org/jira/browse/SUREFIRE-1414
  # Post-Proguard test (verifies Proguard settings)
  mvn test -e
  STATUS=$?
fi
cd ..
exit "$STATUS"