#!/bin/sh
if [ "${ANDROID}" = "true" ]; then
  MAYBE_ANDROID_FLAG="-Pandroid"
else
  MAYBE_ANDROID_FLAG=""
fi
NO_GIT_PATH="${PATH}"
if [ "${APPVEYOR}" != "" ]; then
  export RANDOM_DOT_ORG_KEY=$(powershell 'Write-Host ($env:random_dot_org_key) -NoNewLine')
  if [ "${OSTYPE}" = "cygwin" ]; then
    # Workaround for a faulty PATH in Appveyor Cygwin (https://github.com/appveyor/ci/issues/1956)
    NO_GIT_PATH=$(echo "${PATH}" | /usr/bin/awk -v RS=':' -v ORS=':' '/git/ {next} {print}')
  fi
fi
cd betterrandom
# Coverage test
PATH="${NO_GIT_PATH}" mvn ${MAYBE_ANDROID_FLAG} clean jacoco:instrument jacoco:prepare-agent \
    test jacoco:restore-instrumented-classes jacoco:report -e -B || exit 1
echo "[unit-tests.sh] Running Proguard."
PATH="${NO_GIT_PATH}" mvn -DskipTests -Dmaven.test.skip=true ${MAYBE_ANDROID_FLAG} \
	pre-integration-test -B && \
echo "[unit-tests.sh] Testing against Proguarded jar." && \
PATH="${NO_GIT_PATH}" mvn -Dmaven.main.skip=true ${MAYBE_ANDROID_FLAG} integration-test -e -B
STATUS=$?
cd ..
