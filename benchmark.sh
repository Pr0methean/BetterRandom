#!/bin/sh
JVMARGS="-jar benchmarks.jar -f 1 -foe true -v EXTRA"

#Workaround for https://sourceforge.net/p/proguard/bugs/578/
# JVMARGS="$JVMARGS -Xverify:none"

cd betterrandom
mvn -DskipTests clean package proguard:proguard install &&\
cd ../benchmark &&\
mvn -DskipTests clean package &&\
cd target &&\
if [ "$TRAVIS" = "true" ]; then
    java "$JVMARGS" -t 1 &&\
    java "$JVMARGS" -t 2 &&\
else
    java "$JVMARGS" -t 1 2>&1 | tee benchmark_results_one_thread.txt &&\
    java "$JVMARGS" -t 2 2>&1 | tee benchmark_results_two_threads.txt &&\
fi && cd ../..