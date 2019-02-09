#!/bin/sh
cd betterrandom
if [ "$#" -ge 1 ]; then
  MAYBE_P="-P"
  MAYBE_RELEASE="release-sign-artifacts"
  OLDVERSION=$(mvn help:evaluate -Dexpression=project.version | sed -n -e '/^\[.*\]/ !{ /^[0-9]/ { p; q } }' | sed 's/version=//')
else
  MAYBE_P=""
  MAYBE_RELEASE=""
fi &&
rm -f release.properties &&\
rm -rf ../../.m2/repository/io/github/pr0methean/betterrandom/ &&\
(
  if [ "$#" -ge 1 ]; then
    mvn versions:set -DnewVersion=$1
    sed -i "s/${OLDVERSION}<!--updateme-->/$1<!--updateme-->/" ../benchmark/pom.xml
    sed -i "s/${OLDVERSION}<!--updateme-->/$1<!--updateme-->/" ../FifoFiller/pom.xml
    git add pom.xml
    git add ../benchmark/pom.xml
    git add ../FifoFiller/pom.xml
    git commit -m "🤖 Update version numbers"
    VERSION_COMMIT=$(git rev-parse HEAD)
  fi
  mvn -DskipTests -Darguments=-DskipTests -Dmaven.test.skip=true -P release-sign-artifacts \
      clean compile pre-integration-test deploy ${MAYBE_P} ${MAYBE_RELEASE}
  if [ $? ]; then
    if [ "$#" -ge 1 ]; then
      cd ..
      ./publish-javadoc.sh
      git tag "BetterRandom-Java7-$1"
      git push origin "BetterRandom-Java7-$1"
      cd betterrandom
      # https://unix.stackexchange.com/a/23244/79452
      n=${1##*[!0-9]}; p=${1%%$n}
      NEWVERSION="$p$((n+1))-SNAPSHOT"
      mvn versions:set -DnewVersion=${NEWVERSION}
      rm pom.xml.versionsBackup
      # For some reason we end up with -SNAPSHOT-SNAPSHOT without next 2 lines:
      sed -i "s/$1-SNAPSHOT<!--updateme-->/${NEWVERSION}<!--updateme-->/" ../benchmark/pom.xml
      sed -i "s/$1-SNAPSHOT<!--updateme-->/${NEWVERSION}<!--updateme-->/" ../FifoFiller/pom.xml
      sed -i "s/$1<!--updateme-->/${NEWVERSION}<!--updateme-->/" ../benchmark/pom.xml
      sed -i "s/$1<!--updateme-->/${NEWVERSION}<!--updateme-->/" ../FifoFiller/pom.xml
      git add pom.xml
      git add ../benchmark/pom.xml
      git add ../FifoFiller/pom.xml
      git commit -m "🤖 Update version numbers"
    fi
  else
    if [ "$#" -ge 1 ]; then
      git tag -d "BetterRandom-Java7-$1"
      git push --delete origin "BetterRandom-Java7-$1"
      git revert --no-edit ${VERSION_COMMIT}
      mv pom.xml.versionsBackup pom.xml
      git commit --amend --no-edit
    fi
  fi
  git push
)
cd ..
