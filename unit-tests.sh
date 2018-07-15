#!/bin/sh
if [ "$ANDROID" == 1 ]; then
  MAYBE_ANDROID_FLAG="-Pandroid"
else
  MAYBE_ANDROID_FLAG=""
fi
if [ "${JAVA8}" == "true" ]; then
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
PATH="${NO_GIT_PATH}" mvn ${MAYBE_ANDROID_FLAG} help:active-profiles clean ${MAYBE_JACOCO_PREPARE} \
    test ${MAYBE_JACOCO_REPORT} -e
STATUS=$?
if [ "${STATUS}" == 0 ] && [ "${NO_JACOCO}" != "true" ]; then
  if [ "${TRAVIS}" == "true" ]; then
    COMMIT="$TRAVIS_COMMIT"
    JOB_ID="travis_$TRAVIS_JOB_NUMBER"
  elif [ "${APPVEYOR}" != "" ]; then
    GH_TOKEN=$(powershell 'Write-Host ($env:access_token) -NoNewLine')
    COMMIT="$APPVEYOR_REPO_COMMIT"
    JOB_ID="appveyor_${APPVEYOR_BUILD_NUMBER}.${APPVEYOR_JOB_NUMBER}"
    git config --global user.email "appveyor@appveyor.com"
  else
    # Not in CI
    COMMIT=$(git rev-parse HEAD)
    JOB_ID=$(cat /proc/sys/kernel/random/uuid)
  fi
  git clone https://github.com/Pr0methean/betterrandom-coverage.git
  cd betterrandom-coverage
  if [ -f "${COMMIT}" ]; then
    echo "[unit-tests.sh] Aggregating with JaCoCo reports from other jobs."
    cp "${COMMIT}/*.exec" target
    cp ../pom.xml .
    mvn jacoco:report-aggregate
    rm pom.xml
    JACOCO_DIR="jacoco-aggregate"
  else
    echo "[unit-tests.sh] This is the first JaCoCo report for this build."
    /bin/mkdir "$COMMIT"
    JACOCO_DIR="jacoco"
  fi
  /bin/mv ../target/jacoco.exec "$COMMIT/$JOB_ID.exec"
  cd "$COMMIT"
  git add .
  git commit -m "Coverage report from job $JOB_ID"
  git remote add originauth "https://${GH_TOKEN}@github.com/Pr0methean/betterrandom-coverage.git"
  git push --set-upstream originauth master
  while [ ! $? ]; do
    git pull --rebase  # Merge
    cp "*.exec" "../../target/"
    cp ../../pom.xml .
    mvn "jacoco:report-aggregate"
    rm pom.xml
    git push
  done
  cd ../..
  if [ "${TRAVIS}" == "true" ]; then
    # Coveralls doesn't seem to work in non-.NET Appveyor yet
    # so we have to hope Appveyor pushes its Jacoco reports before Travis does! :(
    mvn coveralls:report
    # Send coverage to Codacy
    wget 'https://github.com/codacy/codacy-coverage-reporter/releases/download/2.0.0/codacy-coverage-reporter-2.0.0-assembly.jar'
    java -jar codacy-coverage-reporter-2.0.0-assembly.jar -l Java -r target/site/${JACOCO_DIR}/jacoco.xml
    # Send coverage to Codecov
    curl -s https://codecov.io/bash | bash
    git config --global user.email "travis@travis-ci.org"
  fi
fi
if [ "${JAVA8}" == "true" ]; then
  echo "[unit-tests.sh] Running Proguard."
  PATH="${NO_GIT_PATH}" mvn -DskipTests -Dmaven.test.skip=true ${MAYBE_ANDROID_FLAG} \
      pre-integration-test && \
      echo "[unit-tests.sh] Testing against Proguarded jar." && \
      PATH="${NO_GIT_PATH}" mvn ${MAYBE_ANDROID_FLAG} integration-test -e
  STATUS=$?
fi
cd ..
exit "$STATUS"
