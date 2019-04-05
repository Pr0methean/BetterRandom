#!/bin/bash
if [ -z "${COVERALLS_TOKEN}" ]; then echo "COVERALLS_TOKEN is unset or blank!"; fi
cd betterrandom
mvn -DrepoToken=${COVERALLS_TOKEN} coveralls:report
