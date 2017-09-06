#!/bin/sh
mvn jacoco:prepare-agent test jacoco:report -pl betterrandom &&\
if [ "$TRAVIS" = "true" ]; then
  mvn coveralls:report -pl betterrandom
fi
