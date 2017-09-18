#!/bin/sh
echo '<!DOCTYPE html><html><head /><body style="font-family: sans-serif;">' > betterrandom/src/main/javadoc/overview.html
ruby ./render-readme-for-javadoc.rb >> betterrandom/src/main/javadoc/overview.html
echo '</body></html>' >> betterrandom/src/main/javadoc/overview.html
cd betterrandom
mvn javadoc:javadoc
cd ..
cp -r betterrandom/target/site/apidocs/* docs
cd docs

# Disable frames
mv overview-summary.html index.html

# AdSense
sed -i 's/<head>/<head><script async src="\/\/pagead2.googlesyndication.com\/pagead\/js\/adsbygoogle.js"><\/script><script>(adsbygoogle = window.adsbygoogle || []).push({google_ad_client: "ca-pub-9922551172827508", enable_page_level_ads: true});<\/script>/' index.html
find . -iname "*.html" -exec sed -i 's/<li><a href="[^\"]*" target="_top">Frames<\/a><\/li>//' {} \;
find . -iname "*.html" -exec sed -i 's/<li><a href="[^\"]*" target="_top">No&nbsp;Frames<\/a><\/li>//' {} \;


# Google Analytics
find . -iname "*.html" -exec sed -i 's/<head>/<head><!-- Google Tag Manager --><script>(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({"gtm.start": new Date().getTime(),event:"gtm.js"});var f=d.getElementsByTagName(s)[0], j=d.createElement(s),dl=l!="dataLayer"?"&l="+l:"";j.async=true;j.src="https:\/\/www.googletagmanager.com\/gtm.js?id="+i+dl;f.parentNode.insertBefore(j,f);})(window,document,"script","dataLayer","GTM-K9NNCTT");<\/script><!-- End Google Tag Manager -->/' {} \;
find . -iname "*.html" -exec sed -i 's/<body>/<body><!-- Google Tag Manager (noscript) --><noscript><iframe src="https:\/\/www.googletagmanager.com\/ns.html?id=GTM-K9NNCTT" height="0" width="0" style="display:none;visibility:hidden"><\/iframe><\/noscript><!-- End Google Tag Manager (noscript) -->/' {} \;

git add .
git commit -m "🤖 Update Javadocs for GitHub Pages"
git push
cd ..