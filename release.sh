#!/bin/sh
cd betterrandom &&\
rm -f release.properties &&\
rm -rf ../../.m2/repository/io/github/pr0methean/betterrandom/ &&\
mvn release:clean &&\
mvn release:prepare -DpreparationGoals="clean package proguard:proguard verify" &&\
mvn release:perform &&\
cd ..
