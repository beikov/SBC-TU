mvn -f $(dirname $0)./../pom.xml install
mvn -f $(dirname $0)./../sbc-gui/pom.xml exec:java -Pgui-jms