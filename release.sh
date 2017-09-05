#!/bin/sh
mvn release:clean release:prepare
mvn release:perform
