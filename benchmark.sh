#!/bin/sh
if [ "$ANDROID" = 1 ]; then
  MAYBE_ANDROID_FLAG="-Pandroid"
else
  MAYBE_ANDROID_FLAG=""
fi
if [ "$APPVEYOR" != "" ]; then
  RANDOM_DOT_ORG_KEY=$(powershell 'Write-Host ($env:random_dot_org_key) -NoNewLine')
fi
cd betterrandom
NO_GIT_PATH="${PATH}"
if [ "${APPVEYOR}" != "" ]; then
  export RANDOM_DOT_ORG_KEY=$(powershell 'Write-Host ($env:random_dot_org_key) -NoNewLine')
  if [ "${OSTYPE}" = "cygwin" ]; then
    # Workaround for a faulty PATH in Appveyor Cygwin (https://github.com/appveyor/ci/issues/1956)
    NO_GIT_PATH=$(echo "${PATH}" | /usr/bin/awk -v RS=':' -v ORS=':' '/git/ {next} {print}')
  fi
fi

# This is in a variable for git-merge compatibility with the master branch.
MAYBE_PROGUARD="pre-integration-test"

cd betterrandom
PATH="${NO_GIT_PATH}" mvn -DskipTests -Darguments=-DskipTests\
    -Dmaven.test.skip=true ${MAYBE_ANDROID_FLAG}\
    clean package ${MAYBE_PROGUARD} install &&\
cd ../benchmark &&\
PATH="${NO_GIT_PATH}" mvn -DskipTests ${MAYBE_ANDROID_FLAG} package &&\
cd target &&\
if [ "$TRAVIS" = "true" ]; then
  java -jar benchmarks.jar -f 1 -t 1 -foe true &&\
  java -jar benchmarks.jar -f 1 -t 2 -foe true
else
  java -jar benchmarks.jar -f 1 -t 1 -foe true -v EXTRA 2>&1 |\
      /usr/bin/tee benchmark_results_one_thread.txt &&\
  java -jar benchmarks.jar -f 1 -t 2 -foe true -v EXTRA 2>&1 |\
      /usr/bin/tee benchmark_results_two_threads.txt
fi && cd ../..
