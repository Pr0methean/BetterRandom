#!/bin/sh
sudo renice -10 $$
if [ "${ANDROID}" = 1 ]; then
  MAYBE_ANDROID_FLAG="-Pandroid"
else
  MAYBE_ANDROID_FLAG=""
fi
if [ "${JAVA8}" = "true" ]; then
  echo "[unit-tests.sh] Using Java 8 mode. JaCoCo will run."
  MAYBE_JACOCO_PREPARE="compile jacoco:instrument jacoco:prepare-agent"
  MAYBE_JACOCO_REPORT="jacoco:restore-instrumented-classes jacoco:report"
else
  echo "[unit-tests.sh] Using Java 9+ mode."
  # https://github.com/jacoco/jacoco/issues/663
  NO_JACOCO="true"
  MAYBE_JACOCO_PREPARE=""
  MAYBE_JACOCO_REPORT=""
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
PATH="${NO_GIT_PATH}" mvn ${MAYBE_ANDROID_FLAG} clean ${MAYBE_JACOCO_PREPARE} \
    test ${MAYBE_JACOCO_REPORT} -e
if [ "${JAVA8}" = "true" ]; then
  echo "[unit-tests.sh] Running Proguard."
  PATH="${NO_GIT_PATH}" mvn -DskipTests -Dmaven.test.skip=true ${MAYBE_ANDROID_FLAG} \
      pre-integration-test && \
      echo "[unit-tests.sh] Testing against Proguarded jar." && \
      PATH="${NO_GIT_PATH}" mvn -Dmaven.main.skip=true ${MAYBE_ANDROID_FLAG} integration-test -e
  STATUS=$?
fi
cd ..
exit "$STATUS"
