#!/bin/sh
sudo apt-get -y remove maven2
sudo apt-add-repository ppa:webupd8team/java
sudo apt-get -y update
sudo apt-get -y install maven markdown oracle-java8-installer
sudo apt-get -y autoremove
gem install github-markup
gem install commonmarker
git config --global user.email "4961925+Pr0methean@users.noreply.github.com"
git submodule init
git submodule update
mkdir ~/.m2

# Set up Maven master password
echo -n '<settingsSecurity><master>' >> ~/.m2/settings-security.xml
echo -n "Maven master password: "
read -s password
echo -n $(mvn -emp "${password}") >> ~/.m2/settings-security.xml
echo '</master></settingsSecurity>' >> ~/.m2/settings-security.xml
echo -n "Sonatype password: "
read -s password
echo -n "PGP password: "
read -s pgp_pass
cat settings.xml.part1 $(mvn -ep "${password}") settings.xml.part2 $(mvn -ep "${pgp_pass}") \
    settings.xml.part3 >> ~/.m2/settings.xml
unset password
unset pgp_pass
