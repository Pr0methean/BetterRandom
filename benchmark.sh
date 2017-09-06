#!/bin/sh
mvn clean install proguard:proguard && java -jar benchmark/target/benchmarks.jar