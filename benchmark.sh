#!/bin/sh
if [ "$ANDROID" = 1 ]; then
  MAYBE_ANDROID_FLAG="-Pandroid"
else
  MAYBE_ANDROID_FLAG=""
fi
NO_GIT_PATH="${PATH}"
if [ "${APPVEYOR}" != "" ]; then
  RANDOM_DOT_ORG_KEY=$(powershell 'Write-Host ($env:random_dot_org_key) -NoNewLine')
  if [ "${OSTYPE}" = "cygwin" ]; then
    # Workaround for a faulty PATH in Appveyor Cygwin (https://github.com/appveyor/ci/issues/1956)
    NO_GIT_PATH=`echo "${PATH}" | "C:\cygwin64\usr\bin\awk" -v RS=':' -v ORS=':' '/git/ {next} {print}'`
  fi
fi
cd betterrandom
if [ "$TRAVIS_JDK_VERSION" = "oraclejdk9" ]; then
  mv pom9.xml pom.xml
fi
if ([ "${OSTYPE}" = "cygwin" ] && ); then
  # Workaround for an Appveyor Cygwin path conflict
  NO_GIT_PATH=`echo "${PATH}" | awk -v RS=':' -v ORS=':' '/git/ {next} {print}'`
else
  NO_GIT_PATH="${PATH}"
fi
PATH="${NO_GIT_PATH}" mvn -DskipTests -Darguments=-DskipTests -Dmaven.test.skip=true ${MAYBE_ANDROID_FLAG} clean package install &&\
cd ../benchmark &&\
PATH="${NO_GIT_PATH}" mvn -DskipTests ${MAYBE_ANDROID_FLAG} package &&\
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
