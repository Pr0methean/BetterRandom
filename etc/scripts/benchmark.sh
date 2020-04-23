#!/bin/bash
set -eo pipefail
if [ "$( echo "$1" | grep -q '[A-Za-z]' )" ]; then
  # first arg names the variable that JAVA_HOME is copied from
  export JAVA_HOME=${!1} # Bashism (https://github.com/koalaman/shellcheck/wiki/SC2039)
fi
JAVA_BIN=${JAVA_HOME}/bin/java
JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"
cd betterrandom
mvn -B -DskipTests -Darguments=-DskipTests\
    -Dmaven.test.skip=true \
    clean package
if [ $PROGUARD ]; then
  echo "[benchmark.sh] Running Proguard (PROGUARD=${PROGUARD})."
  mvn -B -Darguments=-DskipTests -Dmaven.test.skip=true proguard:proguard
else
  echo "[benchmark.sh] Proguard not enabled."
fi
mvn install
cd ../benchmark
mvn -B -DskipTests package
cd target
"${JAVA_BIN}" ${JAVA_OPTS} -jar benchmarks.jar $2 -jvm "${JAVA_BIN}" -f 1 -t 1 -foe true -v EXTRA 2>&1
cd ../..

