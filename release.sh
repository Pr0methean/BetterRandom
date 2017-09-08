#!/bin/sh
cd betterrandom &&\
mvn release:clean release:prepare &&\
mvn release:perform &&\
cd ..
