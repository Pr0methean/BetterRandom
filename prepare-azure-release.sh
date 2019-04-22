#!/bin/bash
SONATYPE_PASS=$(echo ${SONATYPE_PASS} | recode ascii..html)
PGP_PASS=$(echo ${PGP_PASS} | recode ascii..html)
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
echo "<settings>
  <servers>
    <server>
      <id>sonatype-nexus-staging</id>
      <username>Pr0methean</username>
      <password>${SONATYPE_PASS}</password>
    </server>
    <server>
      <id>sonatype-nexus-snapshots</id>
      <username>Pr0methean</username>
      <password>${SONATYPE_PASS}</password>
    </server>
    <server>
      <id>gpg.passphrase</id>
      <passphrase>${PGP_PASS}</passphrase>
    </server>
  </servers>
</settings>" > ~/.m2/settings.xml

