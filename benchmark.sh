#!/bin/sh
if [ "$ANDROID" = 1 ]; then
  MAYBE_ANDROID_FLAG="-Pandroid"
else
  MAYBE_ANDROID_FLAG=""
fi
cd betterrandom
mvn -DskipTests $MAYBE_ANDROID_FLAG clean package proguard:proguard install &&\
cd ../benchmark &&\
mvn -DskipTests $MAYBE_ANDROID_FLAG clean package &&\
cd target &&\
if [ "$TRAVIS" = "true" ]; then
    java -jar benchmarks.jar -f 1 -t 1 -foe true -v EXTRA &&\
    java -jar benchmarks.jar -f 1 -t 2 -foe true -v EXTRA
else
    java -jar benchmarks.jar -f 1 -t 1 -foe true -v EXTRA 2>&1 |\
        tee benchmark_results_one_thread.txt &&\
    java -jar benchmarks.jar -f 1 -t 2 -foe true -v EXTRA 2>&1 |\
        tee benchmark_results_two_threads.txt
fi && cd ../..