#!/bin/sh
if [ "$ANDROID" = 1 ]; then
  MAYBE_ANDROID_FLAG="-Pandroid"
else
  MAYBE_ANDROID_FLAG=""
fi
cd betterrandom
mvn -DskipTests ${MAYBE_ANDROID_FLAG} clean package proguard:proguard install &&\
cd ../benchmark &&\
mvn -DskipTests ${MAYBE_ANDROID_FLAG} clean package &&\
cd target &&\
java -jar benchmarks.jar -f 1 -t 1 -foe true -v EXTRA \
    -jvmArgsPrepend "-agentpath:/home/ubuntu/lightweight-java-profiler/build-64/liblagent.so" 2>&1|\
    tee benchmark_results_one_thread.txt &&\
mv traces.txt traces_one_thread.txt &&\
java -jar benchmarks.jar -f 1 -t 2 -foe true -v EXTRA \
    -jvmArgsPrepend "-agentpath:/home/ubuntu/lightweight-java-profiler/build-64/liblagent.so" 2>&1|\ 2>&1 |\
    tee benchmark_results_two_threads.txt &&\
mv traces.txt traces_two_threads.txt
cd ../..