#!/bin/sh
cd betterrandom &&\
mvn clean test package verify &&\
cd ..