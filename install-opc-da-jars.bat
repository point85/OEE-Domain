rem Maven install script for OPC DA dependent jars not in Maven Central
rem j-interop
call mvn install:install-file -Dfile=C:\dev\OEE-Domain\lib\opcda\org.openscada.jinterop.deps-1.1.0.v20130529.jar -DgroupId=org.openscada -DartifactId=jinterop-deps -Dversion=1.1.0.v20130529 -Dpackaging=jar
call mvn install:install-file -Dfile=C:\dev\OEE-Domain\lib\opcda\org.openscada.jinterop.core-1.1.0.v20130529.jar -DgroupId=org.openscada -DartifactId=jinterop-core -Dversion=1.1.0.v20130529 -Dpackaging=jar

rem open scada utgard
call mvn install:install-file -Dfile=C:\dev\OEE-Domain\lib\opcda\utgard_1.4.jar -DgroupId=org.openscada -DartifactId=utgard -Dversion=1.4 -Dpackaging=jar