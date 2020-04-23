#!/bin/bash
set -eo pipefail
if [ "$( echo "$1" | grep -q '[A-Za-z]' )" ]; then
  # first arg names the variable that JAVA_HOME is copied from
  export JAVA_HOME=${!1} # Bashism (https://github.com/koalaman/shellcheck/wiki/SC2039)
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
PATH="${NO_GIT_PATH}" mvn clean jacoco:instrument jacoco:prepare-agent \
    test jacoco:restore-instrumented-classes jacoco:report -e -B || exit 1
if [ "${PROGUARD}" = "true" ]; then
  echo "[unit-tests.sh] Running Proguard."
  PATH="${NO_GIT_PATH}" mvn package proguard:proguard -B
  echo "[unit-tests.sh] Testing against Proguarded jar."
  PATH="${NO_GIT_PATH}" mvn -Dmaven.main.skip=true integration-test -e -B
else
  echo "[unit-tests.sh] Proguard not enabled."
fi
cd ..
