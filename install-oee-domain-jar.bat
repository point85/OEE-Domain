rem Maven install script for OEE Domain jar
rmdir /Q /S C:\maven_repo\org\point85\oee-domain
call mvn clean
call mvn package
rem install jar in local repo
call mvn install:install-file -Dfile=C:/dev/OEE-Domain/target/OEE-Domain-3.1.1.jar -DgroupId=org.point85 -DartifactId=oee-domain -Dversion=3.1.1 -Dpackaging=jar