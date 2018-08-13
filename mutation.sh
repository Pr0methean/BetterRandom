#!/bin/bash
cd betterrandom
set -eo pipefail # TODO: Find a pipefail method that's not Bash-specific
mvn clean test-compile org.pitest:pitest-maven:mutationCoverage 2>&1 | tee >(grep -qv "BUILD FAILURE")
if [ ! $? ]; then
  exit 1
fi
cd ../docs
git remote set-url origin "https://${GH_TOKEN}@github.com/Pr0methean/pr0methean.github.io.git"
git pull --rebase
git checkout origin/master
git pull --rebase
rm -rf betterrandom-pit-reports
cd ../betterrandom/target/pit-reports
SUBFOLDER=$(LC_COLLATE=C; /usr/bin/printf '%s\c' */)
mv ${SUBFOLDER} ../../../docs/betterrandom-pit-reports
cd ../../../docs
git add betterrandom-pit-reports
git commit -m "Update PIT mutation reports"
git push
while [ ! $? ]; do
  git pull --rebase # Merge
  git push
done
