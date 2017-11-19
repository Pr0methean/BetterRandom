#!/bin/sh
cd betterrandom
if ([ "$TRAVIS_JDK_VERSION" = "oraclejdk9" ] || [ "$TRAVIS_JDK_VERSION" = "openjdk9" ]); then
  mv pom9.xml pom.xml
fi
unset RANDOM_DOT_ORG_KEY # Would probably exhaust even our 5Mbit/day limit
mvn compile test-compile org.pitest:pitest-maven:mutationCoverage
