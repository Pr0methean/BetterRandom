#!/bin/bash
set -euxo pipefail
echo "PGPKEY_PATH=${PGPKEY_PATH}"
echo "MVN_MASTER_PASS length ${#MVN_MASTER_PASS}"
echo "SONATYPE_PASS length ${#SONATYPE_PASS}"
echo "PGP_PASS length ${#PGP_PASS}"
MASTER_CRYPT=$(mvn -emp "${MVN_MASTER_PASS}")
echo "<settingsSecurity><master>${MASTER_CRYPT}</master></settingsSecurity>" > ~/.m2/settings-security.xml
SONATYPE_CRYPT=$(mvn -ep "${SONATYPE_PASS}")
PGP_CRYPT=$(mvn -ep "${PGP_PASS}")
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

