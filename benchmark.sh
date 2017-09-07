#!/bin/sh
cd betterrandom
mvn -DskipTests clean package proguard:proguard &&\
cd ../benchmark &&\
mvn -DskipTests clean package &&\
cd ..
java -jar benchmark/target/benchmarks.jar -foe true -v EXTRA