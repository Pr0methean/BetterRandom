#!/bin/sh
if [ "$TRAVIS_OS_NAME" != "osx" ]; then
  sudo mv -f travis-resources/local_policy.jar $JAVA_HOME/jre/lib/security/local_policy.jar
  sudo mv -f travis-resources/US_export_policy.jar $JAVA_HOME/jre/lib/security/US_export_policy.jar
fi