#!/bin/sh
sudo apt-get -y remove maven2
sudo apt-add-repository ppa:webupd8team/java
sudo apt-get -y update
sudo apt-get -y install maven
sudo apt-get -y install oracle-java8-installer
sudo apt-get -y autoremove
