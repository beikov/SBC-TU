call mvn -f %~dp0..\..\pom.xml install
mvn -f %~dp0..\..\sbc-distributor\pom.xml exec:java -Pdistributor-jms