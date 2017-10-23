#!/bin/sh
sudo apt-get -y remove maven2
sudo apt-get -y update
sudo apt-get -y install maven markdown openjdk-7-jdk
sudo apt-get -y autoremove
gem install github-markup
gem install commonmarker