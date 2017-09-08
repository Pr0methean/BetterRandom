#!/bin/sh
cd betterrandom &&\
rm -f release.properties &&\
rm -rf ../../.m2/repository/io/github/pr0methean/betterrandom/ &&\
mvn release:clean release:prepare proguard:proguard &&\
mvn release:perform &&\
cd ..
