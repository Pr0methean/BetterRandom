#!/bin/sh
(mvn -DskipTests -Darguments=-DskipTests -Dmaven.test.skip=true package gpg:sign) </dev/null
