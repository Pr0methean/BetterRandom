#!/bin/bash
cd betterrandom
set -eo pipefail # TODO: Find a pipefail method that's not Bash-specific
mvn clean test-compile org.pitest:pitest-maven:mutationCoverage 2>&1 | tee >(grep -qv "BUILD FAILURE")
if [ ! $? ]; then
  exit 1
fi
cd ../docs
git checkout master
git pull --commit
<<<<<<< HEAD
rm -rf betterrandom-java7-pit-reports
=======
rm -rf betterrandom-pit-reports
>>>>>>> 2155155e... Bug fix
cd ../betterrandom/target/pit-reports
SUBFOLDER=$(LC_COLLATE=C; /usr/bin/printf '%s\c' */)
mv ${SUBFOLDER} ../../../docs/betterrandom-java7-pit-reports
cd ../../../docs
git add betterrandom-java7-pit-reports
git commit -m "Update PIT mutation reports"
git remote set-url origin "https://Pr0methean:${GH_TOKEN}@github.com/Pr0methean/pr0methean.github.io.git"
git push
while [ ! $? ]; do
  git pull --commit # Merge
  git push
done
