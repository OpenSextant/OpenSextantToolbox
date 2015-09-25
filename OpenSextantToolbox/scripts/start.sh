#!/bin/bash

# this script is only for running manually, see InstallAsService for details on running as a service

#export proxyhost=
#export proxyport=

java -Xmx8G -Dsolr.home=./solr -Dlog4j.configuration=file:./etc/log4j.properties -classpath ./lib/*:./lib/GATE/* org.opensextant.service.OpenSextantServer ./etc/service-config.properties