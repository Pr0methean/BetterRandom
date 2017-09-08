#!/bin/sh
cd betterrandom
mvn -DskipTests clean package proguard:proguard
mvn test
STATUS=$?
cd ..
exit "$STATUS"