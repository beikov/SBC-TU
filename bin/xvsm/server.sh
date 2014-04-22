mvn -f $(dirname $0)./../pom.xml install
mvn -f $(dirname $0)./../pom.xml exec:java -Pxvsm-server -Dmozartspaces.configurationFile=mozartspaces-server.xml