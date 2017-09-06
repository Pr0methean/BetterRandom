#!/bin/sh
mvn clean package proguard:proguard test -pl betterrandom
