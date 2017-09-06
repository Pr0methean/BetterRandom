#!/bin/sh
cd betterrandom
mvn -pl betterrandom -DskipTests clean package proguard:proguard test
cd ..