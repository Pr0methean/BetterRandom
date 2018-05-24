#!/bin/sh
cd betterrandom
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
rm -rf betterrandom-java7-pit-reports
mv ../betterrandom/target/pit-reports betterrandom-java7-pit-reports
git add betterrandom-java7-pit-reports
git commit -m "Update PIT mutation reports (Java 7 branch)"
git push originauth HEAD:master
while [ ! $? ]; do
  git pull --rebase # Merge
  git push
done
