#!/bin/sh
cd betterrandom
mvn -DskipTests clean install proguard:proguard && (
    cd ../benchmark
    mvn clean install
    cd ..
    java -jar benchmark/target/benchmarks.jar) || cd ..