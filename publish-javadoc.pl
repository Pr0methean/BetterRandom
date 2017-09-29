#!/bin/perl
# Perl reimplementation of publish-javadoc.sh (work in progress)
use File::Copy::Recursive qw(fcopy rcopy dircopy);
use File::Copy qw(move);
use File::Find qw(find);
use Thread::Pool;
my $overview_file = 'betterrandom/src/main/javadoc/overview.html'
open(my $overview, '>', $overview_file) or die 'Failed to create '.$overview_file
print $overview '<!DOCTYPE html><html><head /><body style="font-family: sans-serif;">'
print $overview `ruby ./render-readme-for-javadoc.rb`
print $overview '</body></html>'
close $overview
chdir('betterrandom') or die 'betterrandom subfolder missing or inaccessible'
system('mvn', 'javadoc:javadoc')
chdir('..') or die 'Failed to return to root folder'
dircopy('betterrandom/target/site/apidocs', 'docs')
cd docs

# Disable frames, step 1
move('overview-summary.html', 'index.html')

my $global_enable_adsense = '<script async src="\/\/pagead2.googlesyndication.com\/pagead\/js\/adsbygoogle.js"><\/script><script>(adsbygoogle = window.adsbygoogle || []).push({google_ad_client: "ca-pub-9922551172827508", enable_page_level_ads: true});<\/script>'
my $tag_manager_script = '<script>(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({"gtm.start": new Date().getTime(),event:"gtm.js"});var f=d.getElementsByTagName(s)[0], j=d.createElement(s),dl=l!="dataLayer"?"&l="+l:"";j.async=true;j.src="https:\/\/www.googletagmanager.com\/gtm.js?id="+i+dl;f.parentNode.insertBefore(j,f);})(window,document,"script","dataLayer","GTM-K9NNCTT");<\/script>'

my $frame_links = qr/<li><a href="[^\"]*" target="_top">Frames<\/a><\/li>//; s/<li><a href="[^\"]*" target="_top">No&nbsp;Frames<\/a><\/li>/

sub postProcess {
    my $name = $File::Find::name
    my $outname = $name.'.tmp'
    open(my $infile, '<', $name) or die 'Failed to open '.$name
    open(my $outfile, '>', $outname) or die 'Failed to open '.$outname
    while( my $line = <$infile>)  {
        my $outline = $line
        if ($name =~ /index/) {
            $outline =~ s/<head>/<head>$global_enable_adsense/
        }
        $outline =~ s/$frame_links//
    }
}

my $pool = Thread::Pool->new({ workers => 4 })

# Disable frames, step 2, & Google Analytics & Tag Manager
find . -iname "*.html" -exec sed -i '; s/<head>/<head>/; s/<body>/<body><noscript><iframe src="https:\/\/www.googletagmanager.com\/ns.html?id=GTM-K9NNCTT" height="0" width="0" style="display:none;visibility:hidden"><\/iframe><\/noscript>/' {} \;

# Minify & inline
find . -iname "*.html" -exec sh -c 'inliner -i --skip-absolute-urls $0 > $0.tmp' {} \;
find . -iname "*.html.tmp" -exec rename -f "s/html.tmp$/html/" {} \;

git add .
git commit -m "🤖 Update Javadocs for GitHub Pages"
git push
cd ..