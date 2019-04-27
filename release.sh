#!/bin/bash
cd betterrandom
if [[ -n "${VERSION}" ]]; then
  MAYBE_P="-P"
  MAYBE_RELEASE="release-sign-artifacts"
  OLDVERSION=$(mvn help:evaluate -Dexpression=project.version | sed -n -e '/^\[.*\]/ !{ /^[0-9]/ { p; q } }' | sed 's/version=//')
else
  MAYBE_P=""
  MAYBE_RELEASE=""
fi &&
rm -f release.properties &&\
rm -rf ../../.m2/repository/io/github/pr0methean/betterrandom
if [[ -n "${VERSION}" ]]; then
  mvn versions:set -DnewVersion=${VERSION}
  sed -i "s/${OLDVERSION}<!--updateme-->/${VERSION}<!--updateme-->/" ../benchmark/pom.xml
  sed -i "s/${OLDVERSION}<!--updateme-->/${VERSION}<!--updateme-->/" ../FifoFiller/pom.xml
  git add pom.xml
  git add ../benchmark/pom.xml
  git add ../FifoFiller/pom.xml
  git commit -m "🤖 Update version numbers"
  VERSION_COMMIT=$(git rev-parse HEAD)
fi
mvn -DskipTests -Darguments=-DskipTests -Dmaven.test.skip=true -P!jdk9 -P release-sign-artifacts \
    clean compile pre-integration-test deploy ${MAYBE_P} ${MAYBE_RELEASE}
STATUS=$?
if [[ -n "${VERSION}" ]]; then
  if [ ${STATUS} -eq 0 ]; then
    git checkout "${BRANCH}"
    cd ..
    ./publish-javadoc.sh
    git tag "BetterRandom-${VERSION}"
    git push origin "BetterRandom-${VERSION}"
    cd betterrandom
    # https://unix.stackexchange.com/a/23244/79452
    n=${1##*[!0-9]}; p=${1%%$n}
    NEWVERSION="$p$((n+1))-SNAPSHOT"
    mvn versions:set -DnewVersion=${NEWVERSION}
    rm pom.xml.versionsBackup
    # For some reason we end up with -SNAPSHOT-SNAPSHOT without next 2 lines:
    sed -i "s/${VERSION}-SNAPSHOT<!--updateme-->/${NEWVERSION}<!--updateme-->/" ../benchmark/pom.xml
    sed -i "s/${VERSION}-SNAPSHOT<!--updateme-->/${NEWVERSION}<!--updateme-->/" ../FifoFiller/pom.xml
    sed -i "s/${VERSION}<!--updateme-->/${NEWVERSION}<!--updateme-->/" ../benchmark/pom.xml
    sed -i "s/${VERSION}<!--updateme-->/${NEWVERSION}<!--updateme-->/" ../FifoFiller/pom.xml
    git add pom.xml
    git add ../benchmark/pom.xml
    git add ../FifoFiller/pom.xml
    git commit -m "🤖 Update version numbers"
    git push
  fi
fi
