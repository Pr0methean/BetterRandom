#!/bin/sh
cd betterrandom
if ([ "$TRAVIS_JDK_VERSION" = "oraclejdk9" ] || [ "$TRAVIS_JDK_VERSION" = "openjdk9" ]); then
  mv pom9.xml pom.xml
fi
mvn clean compile test-compile org.pitest:pitest-maven:mutationCoverage
if [ ! $? ]; then
  exit 1
fi
if [ "$TRAVIS_EVENT_TYPE" = "cron" ]; then
  # Do not update reports from cron builds
  exit
fi
cd ../docs
git remote add originauth "https://${GH_TOKEN}@github.com/Pr0methean/pr0methean.github.io.git"
git pull --rebase originauth master
git checkout originauth/master
rm -rf betterrandom-pit-reports
cd ../betterrandom/target/pit-reports
SUBFOLDER=$(LC_COLLATE=C; /usr/bin/printf '%s\c' */)
mv ${SUBFOLDER} ../../../docs/betterrandom-pit-reports
cd ../../../docs
git add betterrandom-pit-reports
git commit -m "Update PIT mutation reports"
git push originauth HEAD:master
while [ ! $? ]; do
  git pull --rebase # Merge
  git push
done
