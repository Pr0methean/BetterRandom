#!/bin/sh
rm -f release.properties
rm -f betterrandom/release.properties
rm -rf ../.m2/repository/io/github/pr0methean/betterrandom/
mvn release:clean release:prepare &&\
mvn release:perform
