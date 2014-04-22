call mvn -f %~dp0..\..\pom.xml install
mvn -f %~dp0..\..\sbc-xvsm\pom.xml exec:java -Passembler -Dmozartspaces.configurationFile=mozartspaces-client.xml