#!/bin/bash
cd betterrandom || exit 1
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
  mvn versions:set "-DnewVersion=${VERSION}"
  sed -i "s/${OLDVERSION}<!--updateme-->/${VERSION}<!--updateme-->/" ../benchmark/pom.xml
  sed -i "s/${OLDVERSION}<!--updateme-->/${VERSION}<!--updateme-->/" ../FifoFiller/pom.xml
  git add pom.xml
  git add ../benchmark/pom.xml
  git add ../FifoFiller/pom.xml
  git commit -m "🤖 Update version numbers"
fi
mvn -B -DskipTests -Darguments=-DskipTests -Dmaven.test.skip=true -P release-sign-artifacts \
    clean pre-integration-test deploy
STATUS=$?
if [[ -n "${VERSION}" ]]; then
  if [ ${STATUS} -eq 0 ]; then
    cd ..
    ./publish-javadoc.sh
    git tag "BetterRandom-${VERSION}"
    git push origin "BetterRandom-${VERSION}"
    git checkout "${BRANCH}"
    cd betterrandom || exit 1
    # https://unix.stackexchange.com/a/23244/79452
    n=${VERSION##*[!0-9]}; p=${VERSION%%$n}
    NEWVERSION="$p$((n+1))-SNAPSHOT"
    mvn versions:set "-DnewVersion=${NEWVERSION}"
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
