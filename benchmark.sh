#!/bin/sh
JAVA_HOME=${!1} # first arg names the variable that JAVA_HOME is copied from
JAVA_BIN=${JAVA_HOME}/bin/java
if [ "${ANDROID}" = "true" ]; then
  MAYBE_ANDROID_FLAG="-Pandroid"
else
  MAYBE_ANDROID_FLAG=""
fi
JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"
cd betterrandom
mvn -B -DskipTests -Darguments=-DskipTests\
    -Dmaven.test.skip=true ${MAYBE_ANDROID_FLAG}\
    clean install &&\
cd ../benchmark &&\
mvn -B -DskipTests ${MAYBE_ANDROID_FLAG} package &&\
cd target &&\
"${JAVA_BIN}" ${JAVA_OPTS} -jar benchmarks.jar $2 -jvm "${JAVA_BIN}" -f 1 -t 1 -foe true -v EXTRA 2>&1 |\
    tee benchmark_results_one_thread.txt &&\
"${JAVA_BIN}" ${JAVA_OPTS} -jar benchmarks.jar $2 -jvm "${JAVA_BIN}" -f 1 -t 2 -foe true -v EXTRA 2>&1 |\
    tee benchmark_results_two_threads.txt &&\
cd ../..
