#!/bin/sh
if [ "$ANDROID" = 1 ]; then
  MAYBE_ANDROID_FLAG="-Pandroid"
else
  MAYBE_ANDROID_FLAG=""
fi
cd betterrandom
if [ "$TRAVIS_JDK_VERSION" = "oraclejdk9" ]; then
  mv pom9.xml pom.xml
fi
mvn -DskipTests -Darguments=-DskipTests -Dmaven.test.skip=true $MAYBE_ANDROID_FLAG clean package install &&\
cd ../benchmark &&\
mvn -DskipTests ${MAYBE_ANDROID_FLAG} package &&\
cd target &&\
if [ "$TRAVIS" = "true" ]; then
    java -jar benchmarks.jar -f 1 -t 1 -foe true &&\
    java -jar benchmarks.jar -f 1 -t 2 -foe true
else
    java -jar benchmarks.jar -f 1 -t 1 -foe true -v EXTRA 2>&1 |\
        tee benchmark_results_one_thread.txt &&\
    java -jar benchmarks.jar -f 1 -t 2 -foe true -v EXTRA 2>&1 |\
        tee benchmark_results_two_threads.txt
fi && cd ../..