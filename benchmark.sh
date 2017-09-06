#!/bin/sh
cd betterrandom
mvn -DskipTests clean install proguard:proguard && (
    cd ../benchmark
    mvn clean install
    cd ..
    java -jar benchmark/target/benchmarks.jar 2>&1 |\
        tee benchmark/target/benchmark_results.txt) ||\
cd ..