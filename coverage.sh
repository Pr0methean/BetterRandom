#!/bin/sh
cd betterrandom
mvn -pl betterrandom jacoco:prepare-agent test jacoco:report &&\
if [ "$TRAVIS" = "true" ]; then
  mvn coveralls:report
fi
cd ..