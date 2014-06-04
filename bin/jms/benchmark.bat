call mvn -f %~dp0..\..\pom.xml install
mvn -f %~dp0..\..\sbc-jms\pom.xml exec:java -Pbenchmark