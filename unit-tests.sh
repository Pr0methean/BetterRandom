#!/bin/bash
if [ "$( echo "$1" | grep -q '[A-Za-z]' )" ]; then
  # first arg names the variable that JAVA_HOME is copied from
  export JAVA_HOME=${!1} # Bashism (https://github.com/koalaman/shellcheck/wiki/SC2039)
fi
if [ "${ANDROID}" = "true" ]; then
  MAYBE_ANDROID_FLAG=-Pandroid
else
  MAYBE_ANDROID_FLAG=
fi
if [ "${JAVA8}" = "true" ]; then
  echo "[unit-tests.sh] Using Java 8 mode. JaCoCo will run."
else
  echo "[unit-tests.sh] Using Java 9+ mode."
fi
NO_GIT_PATH="${PATH}"
if [ "${APPVEYOR}" != "" ]; then
  RANDOM_DOT_ORG_KEY=$(powershell 'Write-Host ($env:random_dot_org_key) -NoNewLine')
  export RANDOM_DOT_ORG_KEY
  if [ "${OSTYPE}" = "cygwin" ]; then
    # Workaround for a faulty PATH in Appveyor Cygwin (https://github.com/appveyor/ci/issues/1956)
    NO_GIT_PATH=$(echo "${PATH}" | /usr/bin/awk -v RS=':' -v ORS=':' '/git/ {next} {print}')
  fi
fi
cd betterrandom || exit
# Coverage test
# DO NOT quote ${MAYBE_ANDROID_FLAG} due to https://issues.apache.org/jira/browse/MNG-6858
PATH="${NO_GIT_PATH}" mvn ${MAYBE_ANDROID_FLAG} clean jacoco:instrument jacoco:prepare-agent \
    test jacoco:restore-instrumented-classes jacoco:report -e -B || exit 1
if [ "${JAVA8}" = "true" ]; then
  echo "[unit-tests.sh] Running Proguard."
  PATH="${NO_GIT_PATH}" mvn -DskipTests -Dmaven.test.skip=true ${MAYBE_ANDROID_FLAG} \
      pre-integration-test -B && \
      echo "[unit-tests.sh] Testing against Proguarded jar." && \
      PATH="${NO_GIT_PATH}" mvn -Dmaven.main.skip=true ${MAYBE_ANDROID_FLAG} integration-test -e -B
fi
cd ..
