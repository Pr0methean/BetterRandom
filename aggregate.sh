#!/bin/bash
ls -R target
mv etc/jacoco_merge.xml ./pom.xml
mvn "jacoco:report-aggregate"
