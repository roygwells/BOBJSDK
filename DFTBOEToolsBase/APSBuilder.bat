
SET BOLIBPATH=C:\Program Files (x86)\SAP BusinessObjects\SAP BusinessObjects Enterprise XI 4.0\java\lib\
SET BOCLASSPATH="%BOLIBPATH%\bcm.jar";"%BOLIBPATH%\biarengine.jar";"%BOLIBPATH%\ceaspect.jar";"%BOLIBPATH%\cecore.jar";"%BOLIBPATH%\celib.jar";"%BOLIBPATH%\ceplugins_core.jar";"%BOLIBPATH%\cesession.jar";"%BOLIBPATH%\cobaidl.jar";"%BOLIBPATH%\ebus405.jar";"%BOLIBPATH%\logging.jar";"%BOLIBPATH%\TraceLog.jar";"%BOLIBPATH%\biplugins.jar";"%BOLIBPATH%\sdk.core.jar";"%BOLIBPATH%\sdk.core.server.common.jar";"%BOLIBPATH%\sdk.core.server.corba.jar";"%BOLIBPATH%\sdk.core.server.jar";"%BOLIBPATH%\sdk.core.session.cms.jar";"%BOLIBPATH%\sdk.core.session.jar";"%BOLIBPATH%\SharedObjects.jar"
SET CLASSPATH="%BOLIBPATH%*"
JAVA -jar APSBuilder.jar
REM java -jar APSBuilder.jar
