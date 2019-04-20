#!/bin/bash
sudo apt-get -y install markdown dieharder
gem install github-markup
gem install commonmarker
git config --global user.email "4961925+Pr0methean@users.noreply.github.com"
mkdir ~/.m2

# Set up passwords
gpg --import ${PGPKEY_PATH}
rm ${PGPKEY_PATH}

echo -n '<settingsSecurity><master>' > ~/.m2/settings-security.xml
echo -n $(mvn -emp "${MVN_MASTER_PASS}") >> ~/.m2/settings-security.xml
echo '</master></settingsSecurity>' >> ~/.m2/settings-security.xml
password_crypt=$(mvn -ep "${SONATYPE_PASS}")
read -s -p "Sonatype PGP password: " pgp_pass
echo ""
rm ~/.m2/settings.xml
cat settings.xml.part1 > ~/.m2/settings_temp.xml
echo -n "${password_crypt}" >> ~/.m2/settings_temp.xml
cat settings.xml.part2 >> ~/.m2/settings_temp.xml
echo -n "${password_crypt}" >> ~/.m2/settings_temp.xml
unset password_crypt
cat settings.xml.part3 >> ~/.m2/settings_temp.xml
echo -n $(mvn -ep "${PGP_PASS}") >> ~/.m2/settings_temp.xml
cat settings.xml.part4 >> ~/.m2/settings_temp.xml
mv ~/.m2/settings_temp.xml ~/.m2/settings.xml
