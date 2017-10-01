#!/bin/sh
if [ "$ANDROID" = 1 ]; then
  MAYBE_ANDROID_FLAG="-Pandroid"
else
  MAYBE_ANDROID_FLAG=""
fi
cd betterrandom
# Coverage test
mvn $MAYBE_ANDROID_FLAG clean jacoco:prepare-agent test jacoco:report -e
STATUS=$?
if [ "$STATUS" = 0 ]; then
  if ([ "$TRAVIS" = "true" ] || [ "$APPVEYOR" != "" ] ); then
    if [ "$TRAVIS" = "true" ]; then
      COMMIT="$TRAVIS_COMMIT"
      JOBID="travis_$TRAVIS_JOB_NUMBER"
      git config --global user.email "travis@travis-ci.org"
      git remote add originauth "https://${GH_TOKEN}@github.com/Pr0methean/betterrandom-coverage.git"
      PUSHPARAM="--set-upstream originauth"
    else
      COMMIT="$APPVEYOR_REPO_COMMIT"
      JOBID="appveyor_$APPVEYOR_JOB_NUMBER"
      git config --global user.email "appveyor@appveyor.com"
      PUSHPARAM=""
    fi
    git clone https://github.com/Pr0methean/betterrandom-coverage.git
    mkdir -p "betterrandom-coverage/$COMMIT"
    cp target/jacoco.exec "betterrandom-coverage/$COMMIT/$JOBID.exec"
    cp ../travis-resources/jacoco_merge.xml betterrandom-coverage/$COMMIT/pom.xml
    cd betterrandom-coverage/$COMMIT
    git add .
    git commit -m "Coverage report from job $JOBID"
    git push $PUSHPARAM
    while [ ! $? ]; do
      git pull --commit  # Merge
      git push
    done
    mvn jacoco:merge
    mv target/jacoco.exec ../../target
    cd ../..
    mvn coveralls:report
  fi
  mvn -DskipTests $MAYBE_ANDROID_FLAG package && (
  # Post-Proguard test (verifies Proguard settings)
  mvn $MAYBE_ANDROID_FLAG test -e)
  STATUS=$?
fi
cd ..
exit "$STATUS"