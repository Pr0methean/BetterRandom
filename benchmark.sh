#!/bin/sh
cd betterrandom
mvn -DskipTests clean package proguard:proguard install &&\
cd ../benchmark &&\
mvn -DskipTests clean package &&\
cd target &&\
java -jar benchmarks.jar -f 1 -t 1 -foe true -v EXTRA 2>&1 |\
    tee benchmark_results_one_thread.txt &&\
java -jar benchmarks.jar -f 1 -t 2 -foe true -v EXTRA 2>&1 |\
    tee benchmark_results_two_threads.txt &&\
cd ../..