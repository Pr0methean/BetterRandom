#!/bin/sh
mv betterrandom/pom.xml.releaseBackup betterrandom/pom.xml
rm betterrandom/release.properties
git add betterrandom/pom.xml
git add betterrandom/release.properties
git commit -m "🤖 Roll back version increment from failed release"
