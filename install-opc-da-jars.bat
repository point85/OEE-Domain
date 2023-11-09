rem Maven install script for OPC DA dependent jars stored locally
call mvn install:install-file -Dfile=C:\dev\OEE-Domain\lib\opcda\org.openscada.external.jcifs-1.2.25.jar -DgroupId=org.openscada -DartifactId=opcda.jcifs -Dversion=1.2.25 -Dpackaging=jar
call mvn install:install-file -Dfile=C:\dev\OEE-Domain\lib\opcda\org.openscada.jinterop.core-2.1.8.jar -DgroupId=org.openscada -DartifactId=opcda.core -Dversion=2.1.8 -Dpackaging=jar
call mvn install:install-file -Dfile=C:\dev\OEE-Domain\lib\opcda\org.openscada.jinterop.deps-1.5.0.jar -DgroupId=org.openscada -DartifactId=opcda.deps -Dversion=1.5.0 -Dpackaging=jar
call mvn install:install-file -Dfile=C:\dev\OEE-Domain\lib\opcda\org.openscada.opc.dcom-1.5.0.jar -DgroupId=org.openscada -DartifactId=opcda.dcom -Dversion=1.5.0 -Dpackaging=jar
call mvn install:install-file -Dfile=C:\dev\OEE-Domain\lib\opcda\org.openscada.opc.lib-1.5.0.jar  -DgroupId=org.openscada -DartifactId=opcda.lib  -Dversion=1.5.0 -Dpackaging=jar