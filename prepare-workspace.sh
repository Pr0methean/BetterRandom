#!/bin/bash
sudo apt-get -y remove maven2
sudo apt-add-repository ppa:webupd8team/java
sudo apt-get -y update
sudo apt-get -y install maven markdown oracle-java8-installer dieharder
sudo apt-get -y autoremove
gem install travis -v 1.8.8 --no-rdoc --no-ri
gem install github-markup
gem install commonmarker
git config --global user.email "4961925+Pr0methean@users.noreply.github.com"
git submodule init
git submodule update
mkdir ~/.m2

# Set up passwords
wget https://www.dropbox.com/s/bcdmr1r9f78j8wu/gpgkey?dl=0 -O ~/gpgkey
gpg --import ~/gpgkey
rm ~/gpgkey
read -s -p "Maven master password: " password
echo ""
echo -n '<settingsSecurity><master>' > ~/.m2/settings-security.xml
echo -n $(mvn -emp "${password}") >> ~/.m2/settings-security.xml
echo '</master></settingsSecurity>' >> ~/.m2/settings-security.xml
read -s -p "Sonatype password: " password
echo ""
password_crypt=$(mvn -ep "${password}")
unset password
read -s -p "Sonatype PGP password: " pgp_pass
echo ""
rm ~/.m2/settings.xml
cat settings.xml.part1 > ~/.m2/settings_temp.xml
echo -n "${password_crypt}" >> ~/.m2/settings_temp.xml
cat settings.xml.part2 >> ~/.m2/settings_temp.xml
echo -n "${password_crypt}" >> ~/.m2/settings_temp.xml
cat settings.xml.part3 >> ~/.m2/settings_temp.xml
echo -n $(mvn -ep "${pgp_pass}") >> ~/.m2/settings_temp.xml
cat settings.xml.part4 >> ~/.m2/settings_temp.xml
mv ~/.m2/settings_temp.xml ~/.m2/settings.xml
unset pgp_pass

# Enable JAVA8 mode for scripts
echo "export JAVA8=true" >> ~/.bashrc
export JAVA8=true
