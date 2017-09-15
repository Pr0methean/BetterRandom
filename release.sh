#!/bin/sh
cd betterrandom &&\
OLDVERSION=`mvn help:evaluate -Dexpression=project.version | sed -n -e '/^\[.*\]/ !{ /^[0-9]/ { p; q } }' | sed 's/version=//'`
rm -f release.properties &&\
rm -rf ../../.m2/repository/io/github/pr0methean/betterrandom/ &&\
(
  mvn release:clean &&\
  mvn release:prepare -X &&\
  mvn release:perform -X -Dmaven.test.skip=true
) || (
    NEWVERSION=`mvn help:evaluate -Dexpression=project.version | sed -n -e '/^\[.*\]/ !{ /^[0-9]/ { p; q } }' | sed 's/version=//'`
    if [ "$NEWVERSION" != "$OLDVERSION" ]; then
      git tag -d "BetterRandom-$NEWVERSION"
      git push origin ":refs/tags/BetterRandom-$NEWVERSION"
      mvn versions:set "-DoldVersion=$NEWVERSION" "-DnewVersion=$OLDVERSION"
      rm -f release.properties
      rm -f pom.xml.versionsBackup
      rm -f pom.xml.releaseBackup
      git add pom.xml
      git commit -m "Roll back version increment from failed release"
      git push
    fi
)
cd ..