rem Maven install script for OEE Domain jar
call mvn -v
call mvn clean package
rem install jar in local repo
call mvn install:install-file -Dfile=./target/OEE-Domain-3.12.3.jar -DgroupId=org.point85 -DartifactId=oee-domain -Dversion=3.12.3 -Dpackaging=jar