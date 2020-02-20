#!/bin/bash
set -euxo pipefail
echo '[prepare-azure-release.sh] Configuring Apt...'
# Microsoft FIXME: Some of the Apt caches in this image lead to 404s!
sudo rm -r /var/lib/apt/lists
sudo mkdir /var/lib/apt/lists
sudo rm /var/cache/apt/*.bin
sudo apt-get update
echo '[prepare-azure-release.sh] Installing Apt packages...'
sudo apt-get -y install ruby-dev markdown recode
echo '[prepare-azure-release.sh] Installing Gems...'
sudo gem install github-markup commonmarker
echo '[prepare-azure-release.sh] Configuring Git...'
mv "$AGENT_TEMPDIRECTORY/.gitconfig" ~
mv "$AGENT_TEMPDIRECTORY/.git-credentials" ~
# Set up passwords
echo '[prepare-azure-release.sh] Configuring GnuPG...'
gpg --import "${PGPKEY_PATH}" </dev/null
rm "${PGPKEY_PATH}"
echo '[prepare-azure-release.sh] Configuring Maven...'
mkdir -p ~/.m2/repository
mv "$AGENT_TEMPDIRECTORY/settings.xml" ~/.m2
mv "$AGENT_TEMPDIRECTORY/settings-security.xml" ~/.m2
