#!/bin/sh
mvn jacoco:prepare-agent test jacoco:report
if $TRAVIS; then
  mvn coveralls:report
fi
