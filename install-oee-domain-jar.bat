rem Maven install script for OEE Domain jar
rmdir /Q /S C:\maven_repo\org\point85\oee-domain
call mvn -v
call mvn clean package
rem install jar in local repo
call mvn install:install-file -Dfile=./target/OEE-Domain-3.10.0.jar -DgroupId=org.point85 -DartifactId=oee-domain -Dversion=3.10.0 -Dpackaging=jar