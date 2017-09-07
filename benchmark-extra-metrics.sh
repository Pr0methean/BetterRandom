#!/bin/sh
cd betterrandom
mvn -DskipTests clean package proguard:proguard &&\
cd ../benchmark &&\
mvn -DskipTests clean package &&\
cd .. &&\
java -cp benchmark/target/benchmarks.jar \
    io.github.pr0methean.betterrandom.benchmark.AbstractRandomBenchmark \
    -v EXTRA