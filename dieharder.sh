#!/bin/sh
cd betterrandom
if ([ "$TRAVIS_JDK_VERSION" = "oraclejdk9" ] || [ "$TRAVIS_JDK_VERSION" = "openjdk9" ]); then
  mv pom9.xml pom.xml
fi
mvn -DskipTests -Darguments=-DskipTests -Dmaven.test.skip=true clean package install
cd ../FifoFiller
mvn package
JAR=$(find target -iname '*-with-dependencies.jar')
mkfifo prng_out
java -jar "${JAR}" io.github.pr0methean.betterrandom.prng.${CLASS} prng_out &\
(dieharder -Y 1 -k 2 -a -g 200 < prng_out)
