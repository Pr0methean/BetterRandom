#!/bin/sh
cd betterrandom
mvn -DskipTests clean package
mvn test
STATUS=$?
cd ..
exit "$STATUS"