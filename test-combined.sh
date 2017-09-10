#!/bin/sh
cd betterrandom &&\
mvn clean test package proguard:proguard verify &&\
cd ..