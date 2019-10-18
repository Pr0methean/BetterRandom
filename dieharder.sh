#!/bin/sh
ROOT_SHELL=$$
JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"
JAVA_BIN="${JAVA_HOME}/bin/java"
if [ "${JAVA8}" = "true" ]; then
  echo "[dieharder.sh] Using Java 8 mode. Running Proguard."
  MAYBE_PROGUARD="pre-integration-test"
else
  echo "[dieharder.sh] Using Java 9+ mode."
  MAYBE_PROGUARD=""
fi
cd betterrandom || exit 1
mvn -B -DskipTests -Darguments=-DskipTests -Dmaven.test.skip=true\
    clean package ${MAYBE_PROGUARD} install 2>&1
cd ../FifoFiller || exit 1
mvn -B package 2>&1
JAR=$(find target -iname '*-with-dependencies.jar')

# Checked Dieharder invocation

chkdh() {
    dieharder -S 1 -g 200 $@
}
"${JAVA_BIN}" ${JAVA_OPTS} -jar "${JAR}" io.github.pr0methean.betterrandom.prng.${CLASS} prng_out ${SEED} 2>&1 &
JAVA_PROCESS=$!
(
  chkdh -Y 1 -k 2 -d 0
  chkdh -Y 1 -k 2 -d 1
  chkdh -Y 1 -k 2 -d 2
  chkdh -Y 1 -k 2 -d 3
  chkdh -Y 1 -k 2 -d 4
  chkdh -Y 1 -k 2 -d 5
  chkdh -Y 1 -k 2 -d 6
  chkdh -Y 1 -k 2 -d 7
  chkdh -Y 1 -k 2 -d 8
  chkdh -Y 1 -k 2 -d 9
  chkdh -Y 1 -k 2 -d 10
  chkdh -Y 1 -k 2 -d 11
  chkdh -Y 1 -k 2 -d 12
  chkdh -Y 1 -k 2 -d 13
  # Marked "Do Not Use": chkdh -Y 1 -k 2 -d 14
  chkdh -Y 1 -k 2 -d 15
  chkdh -Y 1 -k 2 -d 16
  chkdh -Y 1 -k 2 -d 17
  chkdh -Y 1 -k 2 -d 100
  chkdh -Y 1 -k 2 -d 101
  chkdh -d 102
  chkdh -Y 1 -k 2 -d 200 -n 1
  chkdh -Y 1 -k 2 -d 200 -n 2
  chkdh -Y 1 -k 2 -d 200 -n 3
  chkdh -Y 1 -k 2 -d 200 -n 4
  chkdh -Y 1 -k 2 -d 200 -n 5
  chkdh -Y 1 -k 2 -d 200 -n 6
  chkdh -Y 1 -k 2 -d 200 -n 7
  chkdh -Y 1 -k 2 -d 200 -n 8
  chkdh -Y 1 -k 2 -d 200 -n 9
  chkdh -Y 1 -k 2 -d 200 -n 10
  chkdh -Y 1 -k 2 -d 200 -n 11
  chkdh -Y 1 -k 2 -d 200 -n 12
  chkdh -Y 1 -k 2 -d 201 -n 2
  chkdh -Y 1 -k 2 -d 201 -n 3
  chkdh -Y 1 -k 2 -d 201 -n 4
  chkdh -Y 1 -k 2 -d 201 -n 5
  chkdh -Y 1 -k 2 -d 202
  # test 203 takes too long with full psamples, and has many false failures with 30-40 psamples
  chkdh -Y 1 -k 2 -d 204
  chkdh -d 205
  chkdh -d 206
  chkdh -d 207
  chkdh -d 208
  chkdh -d 209
) 2>&1 < prng_out | tee ../dieharder.txt /proc/${ROOT_SHELL}/fd/1 | (grep -m 1 'FAILED' &&\
(
  pkill dieharder
  pkill java
  exit 1
))
