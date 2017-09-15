#!/bin/sh
cd betterrandom &&\
OLDVERSION=version=`mvn help:evaluate -Dexpression=project.version | sed -n -e '/^\[.*\]/ !{ /^[0-9]/ { p; q } }'`
rm -f release.properties &&\
rm -rf ../../.m2/repository/io/github/pr0methean/betterrandom/ &&\
(mvn release:clean &&\
mvn release:prepare -X &&\
mvn release:perform -X) ||\
(
    NEWVERSION=version=`mvn help:evaluate -Dexpression=project.version | sed -n -e '/^\[.*\]/ !{ /^[0-9]/ { p; q } }'`
    if [ "$NEWVERSION" != "$OLDVERSION" ]; then
      git tag -d "BetterRandom-${NEWVERSION}"
      git push origin ":refs/tags/BetterRandom-${NEWVERSION}"
      mvn versions:set $OLDVERSION
    fi
)
cd ..