#!/bin/bash
mkdir target
mv $(System.ArtifactsDirectory)/*.exec target
ls -R target
mv etc/jacoco_merge.xml ./pom.xml
mvn "jacoco:report-aggregate"
