#!/bin/sh
mvn -pl betterrandom jacoco:prepare-agent -pl betterrandom test -pl betterrandom jacoco:report &&\
if [ "$TRAVIS" = "true" ]; then
  mvn -pl betterrandom coveralls:report
fi
