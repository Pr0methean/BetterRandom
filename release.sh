#!/bin/sh
cd betterrandom
OLDVERSION=$(mvn help:evaluate -Dexpression=project.version | sed -n -e '/^\[.*\]/ !{ /^[0-9]/ { p; q } }' | sed 's/version=//') &&\
rm -f release.properties &&\
rm -rf ../../.m2/repository/io/github/pr0methean/betterrandom/ &&\
(
  mvn -DskipTests -Darguments=-DskipTests -Dmaven.test.skip=true -P release-sign-artifacts \
      clean compile pre-integration-test deploy nexus-staging:release
  STATUS=$?
  NEWVERSION=$(mvn help:evaluate -Dexpression=project.version | sed -n -e '/^\[.*\]/ !{ /^[0-9]/ { p; q } }' | sed 's/version=//')
  if [ ${STATUS} ]; then
    sed -i "s/${OLDVERSION}/${NEWVERSION}/" ../benchmark/pom.xml
    sed -i "s/${OLDVERSION}/${NEWVERSION}/" ../FifoFiller/pom.xml
    git add ../benchmark/pom.xml
    git add ../FifoFiller/pom.xml
    git commit -m "🤖 Update benchmark to use new snapshot version following release"
    git push
  else
    if [ "$NEWVERSION" != "$OLDVERSION" ]; then
      git tag -d "BetterRandom-$NEWVERSION"
      git push --delete origin "BetterRandom-$NEWVERSION"
      mvn versions:set "-DoldVersion=$NEWVERSION" "-DnewVersion=$OLDVERSION"
      rm -f release.properties pom.xml.versionsBackup pom.xml.releaseBackup
      git add pom.xml
      git commit -m "🤖 Roll back version increment from failed release"
      git push
    fi
  fi
)
cd ..
