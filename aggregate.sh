#!/bin/bash
mv etc/jacoco_merge.xml betterrandom/pom.xml
cd betterrandom
mvn "jacoco:report-aggregate"
