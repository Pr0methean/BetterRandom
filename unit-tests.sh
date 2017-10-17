#!/bin/sh
if [ "$ANDROID" = 1 ]; then
  MAYBE_ANDROID_FLAG="-Pandroid"
else
  MAYBE_ANDROID_FLAG=""
fi
if ([ "$TRAVIS_JDK_VERSION" = "oraclejdk9" ] || [ "$TRAVIS_JDK_VERSION" = "openjdk9" ]); then
  JAVA9="true"
fi
cd betterrandom
if [ "$JAVA9" = "true" ]; then
  mv pom9.xml pom.xml
fi
# Coverage test
mvn ${MAYBE_ANDROID_FLAG} clean jacoco:prepare-agent test jacoco:report -e
STATUS=$?
if [ "$STATUS" = 0 ]; then
  PUSH_JACOCO="true"
  if [ "$TRAVIS" = "true" ]; then
    if [ "$JAVA9" != "true" ]; then
      # Coveralls doesn't seem to work in non-.NET Appveyor yet
      # so we have to hope Appveyor pushes its Jacoco reports before Travis does! :(
      mvn coveralls:report
      
      # Send coverage to Codacy
      wget 'https://github.com/codacy/codacy-coverage-reporter/releases/download/2.0.0/codacy-coverage-reporter-2.0.0-assembly.jar'
      java -jar codacy-coverage-reporter-2.0.0-assembly.jar -l Java -r target/site/jacoco/jacoco.xml
    fi
    COMMIT="$TRAVIS_COMMIT"
    JOB_ID="travis_$TRAVIS_JOB_NUMBER"
    git config --global user.email "travis@travis-ci.org"
  elif [ "$APPVEYOR" != "" ]; then
    GH_TOKEN=$(powershell 'Write-Host ($env:access_token) -NoNewLine')
    COMMIT="$APPVEYOR_REPO_COMMIT"
    JOB_ID="appveyor_$(date +%Y%m%d)_$APPVEYOR_JOB_NUMBER"
    git config --global user.email "appveyor@appveyor.com"
  else
    PUSH_JACOCO="false"
  fi
  if [ "$PUSH_JACOCO" = "true" ]; then
    git clone https://github.com/Pr0methean/betterrandom-coverage.git
    cd betterrandom-coverage
    mkdir -p "$COMMIT"
    mv ../target/jacoco.exec "$COMMIT/$JOB_ID.exec"
    cd "$COMMIT"
    git add .
    git commit -m "Coverage report from job $JOB_ID"
    git remote add originauth "https://${GH_TOKEN}@github.com/Pr0methean/betterrandom-coverage.git"
    git push --set-upstream originauth master
    while [ ! $? ]; do
      git pull --rebase  # Merge
      git push
    done
    mv *.exec ../../target/
    cd ../..
  fi
  mvn -DskipTests -Dmaven.test.skip=true ${MAYBE_ANDROID_FLAG} jacoco:report-aggregate package && (
    # Post-Proguard test (verifies Proguard settings)
    mvn ${MAYBE_ANDROID_FLAG} test -e
  )
  STATUS=$?
fi
cd ..
exit "$STATUS"