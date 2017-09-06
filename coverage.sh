#!/bin/sh
mvn jacoco:prepare-agent -pl betterrandom &&\
mvn test -pl betterrandom &&\
mvn jacoco:report -pl betterrandom &&\
if [ "$TRAVIS" = "true" ]; then
  mvn coveralls:report -pl betterrandom
fi
