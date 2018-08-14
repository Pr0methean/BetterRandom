#!/bin/sh
cd betterrandom
if [ "$#" -ge 1 ]; then
  OLDVERSION=$(mvn help:evaluate -Dexpression=project.version | sed -n -e '/^\[.*\]/ !{ /^[0-9]/ { p; q } }' | sed 's/version=//')
fi &&
rm -f release.properties &&\
rm -rf ../../.m2/repository/io/github/pr0methean/betterrandom/ &&\
(
  if [ "$#" -ge 1 ]; then
    mvn versions:set -DnewVersion=$1
    sed -i "s/${OLDVERSION}/$1/" ../benchmark/pom.xml
    sed -i "s/${OLDVERSION}/$1/" ../FifoFiller/pom.xml
    git add pom.xml
    git add ../benchmark/pom.xml
    git add ../FifoFiller/pom.xml
    git commit -m "🤖 Update version numbers"
    VERSION_COMMIT=$(git rev-parse HEAD)
  fi
  mvn -DskipTests -Darguments=-DskipTests -Dmaven.test.skip=true -P release-sign-artifacts \
      clean compile pre-integration-test deploy nexus-staging:release
  if [ ! $? ]; then
    if [ "$#" -ge 1 ]; then
      git tag -d "BetterRandom-Java7-$1"
      git push --delete origin "BetterRandom-Java7-$1"
      git revert --no-edit ${VERSION_COMMIT}
    fi
  fi
)
cd ..
