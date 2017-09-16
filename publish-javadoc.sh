#!/bin/sh
markdown README.md > betterrandom/src/main/javadoc/overview.html
cd betterrandom
mvn javadoc:javadoc
mv target/site/apidocs ../docs
cd ..
git add docs
git commit -m "🤖 Update Javadocs for GitHub Pages"
git push
