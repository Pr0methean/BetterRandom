#!/bin/bash
cd betterrandom
mvn coveralls:report -DrepoToken=${COVERALLS_TOKEN}
