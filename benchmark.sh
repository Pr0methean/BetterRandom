#!/bin/sh
cd betterrandom
mvn -DskipTests clean package proguard:proguard &&\
cd ../benchmark &&\
mvn -DskipTests clean package &&\
cd ..
java -jar benchmark/target/benchmarks.jar -f 1 -t 1 -foe true -v EXTRA 2>&1 |\
    tee ./benchmark/target/benchmark_results_one_thread.txt
java -jar benchmark/target/benchmarks.jar -f 1 -t 2 -foe true -v EXTRA 2>&1 |\
    tee ./benchmark/target/benchmark_results_two_threads.txt