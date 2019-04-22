#!/bin/bash
echo '[prepare-azure-release.sh] Installing Apt packages...'
sudo apt-get -y install markdown dieharder
echo '[prepare-azure-release.sh] Installing Gems...'
gem install github-markup commonmarker
echo '[prepare-azure-release.sh] Configuring Git...'
git config --global user.email "4961925+Pr0methean@users.noreply.github.com"
# Set up passwords
# FIXME: Why does this hang if run without timeout?
echo '[prepare-azure-release.sh] Configuring PGP...'
timeout 60s gpg --import ${PGPKEY_PATH} </dev/null
rm ${PGPKEY_PATH}
echo '[prepare-azure-release.sh] Configuring Maven...'
mkdir ~/.m2
echo -n '<settingsSecurity><master>' > ~/.m2/settings-security.xml
echo -n $(mvn -emp "${MVN_MASTER_PASS}") >> ~/.m2/settings-security.xml
echo '</master></settingsSecurity>' >> ~/.m2/settings-security.xml
password_crypt=$(mvn -ep "${SONATYPE_PASS}")
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
