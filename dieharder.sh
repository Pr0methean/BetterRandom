#!/bin/sh
if [ "$TRAVIS" = "true" ]; then
  sudo apt-get -qq update
  sudo apt-get install -y dieharder
fi
cd betterrandom
mvn -DskipTests -Darguments=-DskipTests -Dmaven.test.skip=true clean package install
cd ../FifoFiller
mvn package
JAR=$(find target -iname '*-with-dependencies.jar')
mkfifo prng_out
java -jar "${JAR}" io.github.pr0methean.betterrandom.prng.${CLASS} prng_out &\
((
    dieharder -Y 1 -k 2 -d 0 -g 200
    dieharder -Y 1 -k 2 -d 1 -g 200
    dieharder -Y 1 -k 2 -d 2 -g 200
    dieharder -Y 1 -k 2 -d 3 -g 200
    dieharder -Y 1 -k 2 -d 4 -g 200
    dieharder -Y 1 -k 2 -d 5 -g 200
    dieharder -Y 1 -k 2 -d 6 -g 200
    dieharder -Y 1 -k 2 -d 7 -g 200
    dieharder -Y 1 -k 2 -d 8 -g 200
    dieharder -Y 1 -k 2 -d 9 -g 200
    dieharder -Y 1 -k 2 -d 10 -g 200
    dieharder -Y 1 -k 2 -d 11 -g 200
    dieharder -Y 1 -k 2 -d 12 -g 200
    dieharder -Y 1 -k 2 -d 13 -g 200
    # Marked "Do Not Use": dieharder -Y 1 -k 2 -d 14 -g 200
    dieharder -Y 1 -k 2 -d 15 -g 200
    dieharder -Y 1 -k 2 -d 16 -g 200
    dieharder -Y 1 -k 2 -d 17 -g 200
    dieharder -Y 1 -k 2 -d 100 -g 200
    dieharder -Y 1 -k 2 -d 101 -g 200
    dieharder -Y 1 -k 2 -d 102 -g 200
    dieharder -Y 1 -k 2 -d 200 -g 200 -n 1
    dieharder -Y 1 -k 2 -d 200 -g 200 -n 2
    dieharder -Y 1 -k 2 -d 200 -g 200 -n 3
    dieharder -Y 1 -k 2 -d 200 -g 200 -n 4
    dieharder -Y 1 -k 2 -d 200 -g 200 -n 5
    dieharder -Y 1 -k 2 -d 200 -g 200 -n 6
    dieharder -Y 1 -k 2 -d 200 -g 200 -n 7
    dieharder -Y 1 -k 2 -d 200 -g 200 -n 8
    dieharder -Y 1 -k 2 -d 200 -g 200 -n 9
    dieharder -Y 1 -k 2 -d 200 -g 200 -n 10
    dieharder -Y 1 -k 2 -d 200 -g 200 -n 11
    dieharder -Y 1 -k 2 -d 200 -g 200 -n 12
    dieharder -Y 1 -k 2 -d 201 -g 200 -n 2
    dieharder -Y 1 -k 2 -d 201 -g 200 -n 3
    dieharder -Y 1 -k 2 -d 201 -g 200 -n 4
    dieharder -Y 1 -k 2 -d 201 -g 200 -n 5
    dieharder -Y 1 -k 2 -d 202 -g 200
    dieharder -d 203 -g 200 -p 30 -n 0
    dieharder -d 203 -g 200 -p 30 -n 1
    dieharder -d 203 -g 200 -p 30 -n 2
    dieharder -d 203 -g 200 -p 30 -n 3
    dieharder -d 203 -g 200 -p 30 -n 4
    dieharder -d 203 -g 200 -p 30 -n 5
    dieharder -d 203 -g 200 -p 30 -n 6
    dieharder -d 203 -g 200 -p 30 -n 7
    dieharder -d 203 -g 200 -p 30 -n 8
    dieharder -d 203 -g 200 -p 30 -n 9
    dieharder -d 203 -g 200 -p 30 -n 10
    dieharder -d 203 -g 200 -p 30 -n 11
    dieharder -d 203 -g 200 -p 30 -n 12
    dieharder -d 203 -g 200 -p 30 -n 13
    dieharder -d 203 -g 200 -p 30 -n 14
    dieharder -d 203 -g 200 -p 30 -n 15
    dieharder -d 203 -g 200 -p 30 -n 16
    dieharder -d 203 -g 200 -p 30 -n 17
    dieharder -d 203 -g 200 -p 30 -n 18
    dieharder -d 203 -g 200 -p 30 -n 19
    dieharder -d 203 -g 200 -p 30 -n 20
    dieharder -d 203 -g 200 -p 30 -n 21
    dieharder -d 203 -g 200 -p 30 -n 22
    dieharder -d 203 -g 200 -p 30 -n 23
    dieharder -d 203 -g 200 -p 30 -n 24
    dieharder -d 203 -g 200 -p 30 -n 25
    dieharder -d 203 -g 200 -p 30 -n 26
    dieharder -d 203 -g 200 -p 30 -n 27
    dieharder -d 203 -g 200 -p 30 -n 28
    dieharder -d 203 -g 200 -p 30 -n 29
    dieharder -d 203 -g 200 -p 30 -n 30
    dieharder -d 203 -g 200 -p 30 -n 31
    dieharder -d 203 -g 200 -p 30 -n 32
    dieharder -Y 1 -k 2 -d 204 -g 200
    dieharder -d 205 -g 200
    dieharder -d 206 -g 200
    dieharder -d 207 -g 200
    dieharder -d 208 -g 200
    dieharder -d 209 -g 200
) < prng_out)
