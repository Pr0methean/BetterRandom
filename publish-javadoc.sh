#!/bin/sh
echo '<!DOCTYPE html><html><head /><body style="font-family: sans-serif;">' > betterrandom/src/main/javadoc/overview.html
ruby ./render-readme-for-javadoc.rb >> betterrandom/src/main/javadoc/overview.html
echo '</body></html>' >> betterrandom/src/main/javadoc/overview.html
cd betterrandom
mvn javadoc:javadoc
cd ..
rm -rf docs
mv betterrandom/target/site/apidocs docs
sed -i 's$<head>$<head><script async src="//pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"></script>
<script>
  (adsbygoogle = window.adsbygoogle || []).push({
    google_ad_client: "ca-pub-3178057947968979",
    enable_page_level_ads: true
  });
</script>$' docs/index.html
git add docs
git commit -m "🤖 Update Javadocs for GitHub Pages"
git push
