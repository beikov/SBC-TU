call mvn -f %~dp0..\..\pom.xml install
mvn -f %~dp0..\..\pom.xml exec:java -Pjms-server