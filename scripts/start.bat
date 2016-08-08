@echo OFF
REM set proxyhost=somehost.example.com
REM set proxyport=80
REM to java call if proxy needed
REM -Dhttp.proxyHost=%proxyhost% -Dhttp.proxyPort=%proxyport% 

SET SERVICE_HOME=%~dp0..\
cd %SERVICE_HOME%

java.exe -Xmx1G -Dlog4j.configuration=file:.\etc\log4j.properties -Djava.util.logging.config.file=.\etc\restletlog.properties -classpath .\lib\* org.opensextant.tagger.service.OpenSextantServer .\etc\service-config.properties