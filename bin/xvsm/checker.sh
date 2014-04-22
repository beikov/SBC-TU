mvn -f $(dirname $0)./../pom.xml install
mvn -f $(dirname $0)./../sbc-xvsm/pom.xml exec:java -Pchecker -Dmozartspaces.configurationFile=mozartspaces-client.xml