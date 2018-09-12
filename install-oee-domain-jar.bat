rem Maven install script for OEE Domain jar
call mvn clean
call mvn package
rem install jar in local repo
call mvn install:install-file -Dfile=C:/dev/OEE-Domain/target/OEE-Domain-1.1.0.jar -DgroupId=org.point85 -DartifactId=oee-domain -Dversion=1.1.0 -Dpackaging=jar