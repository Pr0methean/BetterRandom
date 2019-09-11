#!/bin/sh
JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"
JAVA_BIN="${JAVA_HOME}/bin/java"
if [ "${JAVA8}" = "true" ]; then
  echo "[dieharder.sh] Using Java 8 mode. Running Proguard."
  MAYBE_PROGUARD="pre-integration-test"
else
  echo "[dieharder.sh] Using Java 9+ mode."
  MAYBE_PROGUARD=""
fi
cd betterrandom
mvn -B -DskipTests -Darguments=-DskipTests -Dmaven.test.skip=true\
    clean package ${MAYBE_PROGUARD} install
cd ../FifoFiller
mvn -B package
JAR=$(find target -iname '*-with-dependencies.jar')
mkfifo prng_out
"${JAVA_BIN}" ${JAVA_OPTS} -jar "${JAR}" io.github.pr0methean.betterrandom.prng.${CLASS} prng_out &\
((
    dieharder -S 1 -Y 1 -k 2 -d 0 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 1 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 2 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 3 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 4 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 5 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 6 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 7 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 8 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 9 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 10 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 11 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 12 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 13 -g 200
    # Marked "Do Not Use": dieharder -S 1 -Y 1 -k 2 -d 14 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 15 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 16 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 17 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 100 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 101 -g 200
    dieharder -S 1 -d 102 -g 200
    dieharder -S 1 -Y 1 -k 2 -d 200 -g 200 -n 1
    dieharder -S 1 -Y 1 -k 2 -d 200 -g 200 -n 2
    dieharder -S 1 -Y 1 -k 2 -d 200 -g 200 -n 3
    dieharder -S 1 -Y 1 -k 2 -d 200 -g 200 -n 4
    dieharder -S 1 -Y 1 -k 2 -d 200 -g 200 -n 5
    dieharder -S 1 -Y 1 -k 2 -d 200 -g 200 -n 6
    dieharder -S 1 -Y 1 -k 2 -d 200 -g 200 -n 7
    dieharder -S 1 -Y 1 -k 2 -d 200 -g 200 -n 8
    dieharder -S 1 -Y 1 -k 2 -d 200 -g 200 -n 9
    dieharder -S 1 -Y 1 -k 2 -d 200 -g 200 -n 10
    dieharder -S 1 -Y 1 -k 2 -d 200 -g 200 -n 11
    dieharder -S 1 -Y 1 -k 2 -d 200 -g 200 -n 12
    dieharder -S 1 -Y 1 -k 2 -d 201 -g 200 -n 2
    dieharder -S 1 -Y 1 -k 2 -d 201 -g 200 -n 3
    dieharder -S 1 -Y 1 -k 2 -d 201 -g 200 -n 4
    dieharder -S 1 -Y 1 -k 2 -d 201 -g 200 -n 5
    dieharder -S 1 -Y 1 -k 2 -d 202 -g 200
    dieharder -S 1 -d 203 -g 200 -p 30 -n 0
    dieharder -S 1 -d 203 -g 200 -p 30 -n 1
    dieharder -S 1 -d 203 -g 200 -p 30 -n 2
    dieharder -S 1 -d 203 -g 200 -p 30 -n 3
    dieharder -S 1 -d 203 -g 200 -p 30 -n 4
    dieharder -S 1 -d 203 -g 200 -p 30 -n 5
    dieharder -S 1 -d 203 -g 200 -p 30 -n 6
    dieharder -S 1 -d 203 -g 200 -p 30 -n 7
    dieharder -S 1 -d 203 -g 200 -p 30 -n 8
    dieharder -S 1 -d 203 -g 200 -p 30 -n 9
    dieharder -S 1 -d 203 -g 200 -p 30 -n 10
    dieharder -S 1 -d 203 -g 200 -p 30 -n 11
    dieharder -S 1 -d 203 -g 200 -p 30 -n 12
    dieharder -S 1 -d 203 -g 200 -p 30 -n 13
    dieharder -S 1 -d 203 -g 200 -p 30 -n 14
    dieharder -S 1 -d 203 -g 200 -p 30 -n 15
    dieharder -S 1 -d 203 -g 200 -p 30 -n 16
    dieharder -S 1 -d 203 -g 200 -p 30 -n 17
    dieharder -S 1 -d 203 -g 200 -p 30 -n 18
    dieharder -S 1 -d 203 -g 200 -p 30 -n 19
    dieharder -S 1 -d 203 -g 200 -p 30 -n 20
    dieharder -S 1 -d 203 -g 200 -p 30 -n 21
    dieharder -S 1 -d 203 -g 200 -p 30 -n 22
    dieharder -S 1 -d 203 -g 200 -p 30 -n 23
    dieharder -S 1 -d 203 -g 200 -p 30 -n 24
    dieharder -S 1 -d 203 -g 200 -p 30 -n 25
    dieharder -S 1 -d 203 -g 200 -p 30 -n 26
    dieharder -S 1 -d 203 -g 200 -p 30 -n 27
    dieharder -S 1 -d 203 -g 200 -p 30 -n 28
    dieharder -S 1 -d 203 -g 200 -p 30 -n 29
    dieharder -S 1 -d 203 -g 200 -p 30 -n 30
    dieharder -S 1 -d 203 -g 200 -p 30 -n 31
    dieharder -S 1 -d 203 -g 200 -p 30 -n 32
    dieharder -S 1 -Y 1 -k 2 -d 204 -g 200
    dieharder -S 1 -d 205 -g 200
    dieharder -S 1 -d 206 -g 200
    dieharder -S 1 -d 207 -g 200
    dieharder -S 1 -d 208 -g 200
    dieharder -S 1 -d 209 -g 200
) < prng_out | tee ../dieharder.txt | tee /dev/tty | [[ ! `grep 'FAILED'` ]])
