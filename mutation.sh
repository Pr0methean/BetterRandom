#!/bin/sh
cd betterrandom
if ([ "$TRAVIS_JDK_VERSION" = "oraclejdk9" ] || [ "$TRAVIS_JDK_VERSION" = "openjdk9" ]); then
  mv pom9.xml pom.xml
fi
unset RANDOM_DOT_ORG_KEY # Would probably exhaust even our 5Mbit/day limit
mvn compile test-compile org.pitest:pitest-maven:mutationCoverage
cd ..
git checkout https://github.com/Pr0methean/pr0methean.github.io docs
rm -rf docs/betterrandom-pit-reports
mv betterrandom/target/pit-reports docs/betterrandom-pit-reports
cd docs
git add betterrandom-pit-reports
git commit -m "Update PIT mutation reports"
git remote add originauth "https://${GH_TOKEN}@github.com/Pr0methean/betterrandom-coverage.git"
git push --set-upstream originauth master
