#!/bin/sh
mvn -pl betterrandom jacoco:prepare-agent test jacoco:report &&\
if [ "$TRAVIS" = "true" ]; then
  mvn -pl betterrandom coveralls:report
fi
