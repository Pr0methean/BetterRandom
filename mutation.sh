#!/bin/sh
cd betterrandom
unset RANDOM_DOT_ORG_KEY # Would probably exhaust even our 5Mbit/day limit
mvn compile org.pitest:pitest-maven:mutationCoverage
