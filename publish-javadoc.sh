#!/bin/sh
rm -r betterrandom/target/site/apidocs
rm -r docs/betterrandom-java7/io
echo '<!DOCTYPE html><html><head /><body style="font-family: sans-serif;">' > betterrandom/src/main/javadoc/overview.html
ruby ./render-readme-for-javadoc.rb >> betterrandom/src/main/javadoc/overview.html
echo '</body></html>' >> betterrandom/src/main/javadoc/overview.html
cd betterrandom || exit 1
mvn javadoc:javadoc
rm src/main/javadoc/overview.html # Only needed temporarily
cd ..
cp -r betterrandom/target/site/apidocs/* docs/betterrandom-java7
cd docs || exit 1
git checkout master
git pull
cd betterrandom-java7 || exit 1

# Create sitemap
find . -iname "*.html" | sed 's/^\./https:\/\/pr0methean.github.io/' > sitemap.txt

# AdSense, Analytics, Tag Manager
replace '<head>' "<head><script>(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':\
new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],\
j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=\
'https://www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);\
})(window,document,'script','dataLayer','GTM-K9NNCTT');</script>\
<script async src='//pagead2.googlesyndication.com\/pagead\/js\/adsbygoogle.js'></script>\
<script>(adsbygoogle = window.adsbygoogle || []).push({google_ad_client: 'ca-pub-9922551172827508', \
enable_page_level_ads: true});</script>" -- index.html

# Add Tidelift link
find . -iname "*.html" -print0 | xargs -r0 replace 'Help</a></li>' 'Help</a></li><li><a href="/betterrandom-enterprise.html">For Enterprise</a></li>' --

git add .
cd ..
find . -iname '*.html' | replace './' 'https://pr0methean.github.io/' > sitemap.txt
git add sitemap.txt
git commit -m "🤖 Update Javadocs for GitHub Pages"
git branch
git pull --commit
git push
cd ..
git submodule update --remote
