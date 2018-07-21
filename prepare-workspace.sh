#!/bin/bash
sudo apt-get -y remove maven2
sudo apt-get -y update
sudo apt-get -y install maven markdown openjdk-7-jdk
sudo apt-get -y autoremove
gem install github-markup
gem install commonmarker
git config --global user.email "4961925+Pr0methean@users.noreply.github.com"
git submodule init
git submodule update
mkdir ~/.m2

# Set up passwords
read -s -p "Maven master password: " password
echo ""
echo -n '<settingsSecurity><master>' > ~/.m2/settings-security.xml
echo -n $(mvn -emp "${password}") >> ~/.m2/settings-security.xml
echo '</master></settingsSecurity>' >> ~/.m2/settings-security.xml
read -s -p "Sonatype password: " password
echo ""
read -s -p "Sonatype PGP password: " pgp_pass
echo ""
cat settings.xml.part1 > ~/.m2/settings.xml
echo $(mvn -ep "${password}") >> ~/.m2/settings.xml
cat settings.xml.part2 >> ~/.m2/settings.xml
echo $(mvn -ep "${pgp_pass}") >> ~/.m2/settings.xml
cat settings.xml.part3 >> ~/.m2/settings.xml
unset password
unset pgp_pass
