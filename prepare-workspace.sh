#!/bin/sh
sudo apt-get -y remove maven2
sudo apt-add-repository ppa:webupd8team/java
sudo apt-get -y update
sudo apt-get -y install maven markdown oracle-java8-installer
sudo apt-get -y autoremove
gem install github-markup
gem install commonmarker
npm install -g inliner