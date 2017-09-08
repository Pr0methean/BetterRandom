#!/bin/sh
sudo apt-get remove maven2
sudo apt-add-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install maven
sudo apt-get install oracle-java8-installer
sudo apt-get autoremove
