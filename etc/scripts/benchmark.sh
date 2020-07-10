#!/bin/bash
set -eo pipefail
JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"
cd betterrandom
mvn -B -DskipTests -Darguments=-DskipTests\
    -Dmaven.test.skip=true \
    clean package
if [ "${PROGUARD}" = "true" ]; then
  echo "[benchmark.sh] Running Proguard (PROGUARD=${PROGUARD})."
  mvn -B -DskipTests -Darguments=-DskipTests -Dmaven.test.skip=true proguard:proguard
else
  echo "[benchmark.sh] Proguard not enabled."
fi
mvn -DskipTests -Darguments=-DskipTests -Dmaven.test.skip=true -Dmaven.main.skip=true install
cd ../benchmark
mvn -B -DskipTests package
cd target
java ${JAVA_OPTS} -jar benchmarks.jar $1 -jvm "${JAVA_BIN}" -f 1 -t 1 -foe true -v EXTRA 2>&1
cd ../..

