#!/bin/sh
cd betterrandom
mvn javadoc:javadoc
mv target/apidocs ../docs
cd ..
git add docs
git commit -m "Update Javadocs for GitHub Pages"
git push
