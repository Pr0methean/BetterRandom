#!/bin/sh
if [ "$ANDROID" = 1 ]; then
  MAYBE_ANDROID_FLAG="-Pandroid"
else
  MAYBE_ANDROID_FLAG=""
fi
cd betterrandom
# Coverage test
if ( [ "$TRAVIS_OS_NAME" != "osx" ] && [ "$APPVEYOR" = "" ] ); then
  mvn $MAYBE_ANDROID_FLAG clean jacoco:prepare-agent test jacoco:report -e
fi
STATUS=$?
if [ "$STATUS" = 0 ]; then
  if [ "$TRAVIS" = "true" ]; then
    git config --global user.email "travis@travis-ci.org"
    git config --global user.name "Travis CI on behalf of Chris Hennick"
    git clone https://github.com/Pr0methean/betterrandom-coverage.git
    mkdir -p "betterrandom-coverage/$TRAVIS_BUILD_NUMBER"
    cp target/jacoco.exec "betterrandom-coverage/$TRAVIS_BUILD_NUMBER/$TRAVIS_JOB_NUMBER.exec"
    cp travis-resources/jacoco_merge.xml betterrandom-coverage/$TRAVIS_BUILD_NUMBER/pom.xml
    cd betterrandom-coverage/$TRAVIS_BUILD_NUMBER
    git add "$TRAVIS_BUILD_NUMBER"
    git commit -m "Coverage report from job $TRAVIS_JOB_NUMBER"
    while [ ! git push ]; do
      git pull --commit  # Merge
    done
    mvn jacoco:merge
    mv target/jacoco.exec ../target
    cd ..
    mvn coveralls:report
  fi
  mvn -DskipTests $MAYBE_ANDROID_FLAG package && (
  # Post-Proguard test (verifies Proguard settings)
  mvn $MAYBE_ANDROID_FLAG test -e)
  STATUS=$?
fi
cd ..
exit "$STATUS"