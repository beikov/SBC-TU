mvn -f $(dirname $0)./../pom.xml install
mvn -f $(dirname $0)./../sbc-jms/pom.xml exec:java -Passembler