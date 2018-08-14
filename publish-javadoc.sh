#!/bin/sh
rm -r betterrandom/target/site/apidocs
rm -r docs/betterrandom-java8/io
echo '<!DOCTYPE html><html><head /><body style="font-family: sans-serif;">' > betterrandom/src/main/javadoc/overview.html
ruby ./render-readme-for-javadoc.rb >> betterrandom/src/main/javadoc/overview.html
echo '</body></html>' >> betterrandom/src/main/javadoc/overview.html
cd betterrandom
mvn javadoc:javadoc
rm src/main/javadoc/overview.html # Only needed temporarily
cd ..
cp -r betterrandom/target/site/apidocs/* docs/betterrandom-java8
cd docs
git checkout master
git pull
cd betterrandom-java8

# Disable frames, step 1
mv overview-summary.html index.html

# Create sitemap
find . -iname "*.html" | sed 's/^\./https:\/\/pr0methean.github.io/' > sitemap.txt

# AdSense
sed -i 's/<head>/<head><script async src="\/\/pagead2.googlesyndication.com\/pagead\/js\/adsbygoogle.js"><\/script><script>(adsbygoogle = window.adsbygoogle || []).push({google_ad_client: "ca-pub-9922551172827508", enable_page_level_ads: true});<\/script>/' index.html

# Disable frames, step 2, & Google Analytics & Tag Manager, and use J2SE 8 instead of 7 links
find . -iname "*.html" -exec sed -i 's/<li><a href="[^\"]*" target="_top">Frames<\/a><\/li>//; s/<li><a href="[^\"]*" target="_top">No&nbsp;Frames<\/a><\/li>//; s/<head>/<head><!-- Google Tag Manager --><script>(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({"gtm.start": new Date().getTime(),event:"gtm.js"});var f=d.getElementsByTagName(s)[0], j=d.createElement(s),dl=l!="dataLayer"?"&l="+l:"";j.async=true;j.src="https:\/\/www.googletagmanager.com\/gtm.js?id="+i+dl;f.parentNode.insertBefore(j,f);})(window,document,"script","dataLayer","GTM-K9NNCTT");<\/script><!-- End Google Tag Manager -->/; s/<body>/<body><noscript><iframe src="https:\/\/www.googletagmanager.com\/ns.html?id=GTM-K9NNCTT" height="0" width="0" style="display:none;visibility:hidden"><\/iframe><\/noscript>/; s/overview-summary.html/index.html/g; s/javase\/7/javase\/8/g' {} \;
git add .
git commit -m "🤖 Update Javadocs for GitHub Pages"
git pull --commit
git push
cd ..
git add docs
git commit -m "🤖 Update submodule"
git push
