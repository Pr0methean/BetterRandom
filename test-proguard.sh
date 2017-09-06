#!/bin/sh
mvn -pl betterrandom clean -pl betterrandom package -pl betterrandom proguard:proguard -pl betterrandom test