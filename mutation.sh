#!/bin/sh
cd betterrandom
unset RANDOM_DOT_ORG_KEY # Would probably exhaust even our 5Mbit/day limit
mvn compile test-compile org.pitest:pitest-maven:mutationCoverage
cd ..
git clone https://github.com/Pr0methean/pr0methean.github.io.git docs