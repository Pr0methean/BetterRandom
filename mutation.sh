#!/bin/sh
cd betterrandom
mvn clean test-compile org.pitest:pitest-maven:mutationCoverage 2>&1 | tee >(grep -qv "BUILD FAILURE")
if [ ! $? ]; then
  exit 1
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
