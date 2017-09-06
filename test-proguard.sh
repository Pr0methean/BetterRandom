#!/bin/sh
mvn clean -pl betterrandom &&\
mvn package -pl betterrandom &&\
mvn proguard:proguard -pl betterrandom &&\
mvn test -pl betterrandom