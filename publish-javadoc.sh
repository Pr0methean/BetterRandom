#!/bin/sh
echo '<!DOCTYPE html><html><head /><body style="font-family: sans-serif;">' > betterrandom/src/main/javadoc/overview.html
ruby ./render-readme-for-javadoc.rb >> betterrandom/src/main/javadoc/overview.html
echo '</body></html>' >> betterrandom/src/main/javadoc/overview.html
cd betterrandom
mvn javadoc:javadoc
cd ..
mv -f betterrandom/target/site/apidocs docs
sed -i 's/<head>/<head><script async src="\/\/pagead2.googlesyndication.com\/pagead\/js\/adsbygoogle.js"><\/script><script>(adsbygoogle = window.adsbygoogle || []).push({google_ad_client: "ca-pub-9922551172827508", enable_page_level_ads: true});<\/script>/' docs/index.html
cd docs
git add .
git commit -m "🤖 Update Javadocs for GitHub Pages"
git push
cd ..