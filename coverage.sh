#!/bin/sh
cd betterrandom
mvn jacoco:prepare-agent test jacoco:report &&\
if [ "$TRAVIS" = "true" ]; then
  mvn coveralls:report
fi
cd ..