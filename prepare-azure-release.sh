#!/bin/bash
set -euxo pipefail
echo "PGPKEY_PATH=${PGPKEY_PATH}"
echo "MVN_MASTER_PASS length ${#MVN_MASTER_PASS}"
echo "SONATYPE_PASS length ${#SONATYPE_PASS}"
echo "PGP_PASS length ${#PGP_PASS}"
echo '[prepare-azure-release.sh] Configuring Apt...'
sudo rm /var/lib/apt/lists/*
sudo rm /var/cache/apt/*.bin
sudo apt-get update
echo '[prepare-azure-release.sh] Installing Apt packages...'
sudo apt-get update
sudo apt-get -y install ruby markdown dieharder recode
echo '[prepare-azure-release.sh] Installing Gems...'
gem install github-markup commonmarker
echo '[prepare-azure-release.sh] Configuring Git...'
git config --global user.email "4961925+Pr0methean@users.noreply.github.com"
git config --global user.name "Chris Hennick"
# Set up passwords
echo '[prepare-azure-release.sh] Configuring GnuPG...'
gpg --import ${PGPKEY_PATH} </dev/null
rm ${PGPKEY_PATH}
echo '[prepare-azure-release.sh] Configuring Maven...'
SONATYPE_PASS=$(echo ${SONATYPE_PASS} | recode ascii..html)
PGP_PASS=$(echo ${PGP_PASS} | recode ascii..html)
mkdir ~/.m2
MASTER_CRYPT=$(mvn -emp ${MVN_MASTER_PASS})
echo "<settingsSecurity><master>${MASTER_CRYPT}</master></settingsSecurity>" > ~/.m2/settings-security.xml
SONATYPE_CRYPT=$(mvn -emp ${SONATYPE_PASS})
PGP_CRYPT=$(mvn -emp ${PGP_PASS})
echo "<settings>
  <servers>
    <server>
      <id>sonatype-nexus-staging</id>
      <username>Pr0methean</username>
      <password>${SONATYPE_CRYPT}</password>
    </server>
    <server>
      <id>sonatype-nexus-snapshots</id>
      <username>Pr0methean</username>
      <password>${SONATYPE_CRYPT}</password>
    </server>
    <server>
      <id>gpg.passphrase</id>
      <passphrase>${PGP_CRYPT}</passphrase>
    </server>
  </servers>
</settings>" > ~/.m2/settings.xml

