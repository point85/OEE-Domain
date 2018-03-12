rem Maven install script for OPC DA dependent jars not in Maven Central
rem j-interop
rem call mvn install:install-file -Dfile=C:\dev\OEE-Domain\lib\opcda\org.openscada.jinterop.deps-1.1.0.v20130529.jar -DgroupId=org.openscada -DartifactId=jinterop-deps -Dversion=1.1.0.v20130529 -Dpackaging=jar
rem call mvn install:install-file -Dfile=C:\dev\OEE-Domain\lib\opcda\org.openscada.jinterop.core-1.1.0.v20130529.jar -DgroupId=org.openscada -DartifactId=jinterop-core -Dversion=1.1.0.v20130529 -Dpackaging=jar

rem open scada utgard
rem call mvn install:install-file -Dfile=C:\dev\OEE-Domain\lib\opcda\utgard_1.4.jar -DgroupId=org.openscada -DartifactId=utgard -Dversion=1.4 -Dpackaging=jar

call mvn install:install-file -Dfile=C:\dev\OEE-Domain\lib\opcda\org.openscada.jinterop.core-2.1.8.v20140625-1417.jar -DgroupId=org.openscada -DartifactId=jinterop-core -Dversion=2.1.8 -Dpackaging=jar
call mvn install:install-file -Dfile=C:\dev\OEE-Domain\lib\opcda\org.openscada.jinterop.deps-1.3.0.v20141013-0754.jar -DgroupId=org.openscada -DartifactId=jinterop-deps -Dversion=1.3.0 -Dpackaging=jar
call mvn install:install-file -Dfile=C:\dev\OEE-Domain\lib\opcda\org.openscada.opc.dcom-1.2.0.v20141105-1322.jar      -DgroupId=org.openscada -DartifactId=opc-dcom      -Dversion=1.2.0 -Dpackaging=jar
call mvn install:install-file -Dfile=C:\dev\OEE-Domain\lib\opcda\org.openscada.opc.lib-1.3.0.v20141118-1249.jar       -DgroupId=org.openscada -DartifactId=opc-lib       -Dversion=1.3.0 -Dpackaging=jar