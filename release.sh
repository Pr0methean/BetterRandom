#!/bin/sh
cd betterrandom &&\
rm -f release.properties &&\
rm -rf ../../.m2/repository/io/github/pr0methean/betterrandom/ &&\
mvn release:clean release:prepare &&\
cd target &&\
# WTF? Why does proguard:proguard still expect -SNAPSHOT in the jar name?!
rename 's/(.*)\.jar$/$1-SNAPSHOT\.jar/' &&\
mvn proguard:proguard &&\
rename 's/(.*)-SNAPSHOT\.jar$/$1\.jar/' &&\
cd .. &&\
mvn release:perform &&\
cd ..
