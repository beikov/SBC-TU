call mvn -f %~dp0..\..\pom.xml install
mvn -f %~dp0..\..\sbc-gui\pom.xml exec:java -Pgui-jms