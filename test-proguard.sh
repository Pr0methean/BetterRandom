#!/bin/sh
cd betterrandom
mvn clean package proguard:proguard test
cd ..