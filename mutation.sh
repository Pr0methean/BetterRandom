#!/bin/sh
cd betterrandom
if ([ "$TRAVIS_JDK_VERSION" = "oraclejdk9" ] || [ "$TRAVIS_JDK_VERSION" = "openjdk9" ]); then
  mv pom9.xml pom.xml
fi
unset RANDOM_DOT_ORG_KEY # Would probably exhaust even our 5Mbit/day limit
mvn compile test-compile org.pitest:pitest-maven:mutationCoverage
cd ../docs
git remote add originauth "https://${GH_TOKEN}@github.com/Pr0methean/betterrandom-coverage.git"
git branch --set-upstream-to originauth/master
git pull --rebase
rm -rf docs/betterrandom-pit-reports
mv ../betterrandom/target/pit-reports betterrandom-pit-reports
git add betterrandom-pit-reports
git commit -m "Update PIT mutation reports"
git push
while [ ! $? ]; do
  git pull --rebase # Merge
  git push
done