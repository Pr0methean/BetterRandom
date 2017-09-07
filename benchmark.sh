#!/bin/sh
cd betterrandom
mvn -DskipTests clean package proguard:proguard &&\
cd ../benchmark &&\
mvn -DskipTests clean package &&\
cd ..
java -jar benchmark/target/benchmarks.jar -f 1 -foe true -v EXTRA 2>&1 |\
    tee ./benchmark/target/benchmark_results.txt