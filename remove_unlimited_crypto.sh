#!/bin/sh
if [ "$TRAVIS_OS_NAME" != "osx" ]; then
  # || true ignores errors, because ${JAVA_HOME}/jre/lib/security isn't in all JDK versions
  sudo mv -f travis-resources/local_policy.jar ${JAVA_HOME}/jre/lib/security/local_policy.jar || true
  sudo mv -f travis-resources/US_export_policy.jar ${JAVA_HOME}/jre/lib/security/US_export_policy.jar || true
fi