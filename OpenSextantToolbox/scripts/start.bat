@echo OFF
SET SERVICE_HOME=%~dp0..\
cd %SERVICE_HOME%

set solr.home=.\solr
java.exe -Xmx1G -Dlog4j.configuration=file:.\etc\log4j.properties -classpath .\lib\*;.\lib\GATE\* org.opensextant.service.OpenSextantServer .\etc\service-config.properties