call mvn -f %~dp0..\..\pom.xml install
mvn -f %~dp0..\..\sbc-common\pom.xml exec:java -Pxvsm-server -Dmozartspaces.configurationFile=mozartspaces-server.xml