#!/bin/bash
cd betterrandom
mvn -DrepoToken=${COVERALLS_TOKEN} coveralls:report 
