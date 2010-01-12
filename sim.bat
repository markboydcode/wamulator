@echo off

if (%1)==() goto fail
goto ok

:fail
echo You must specify a configuration file
goto end

:ok
echo Using configuration file %1

rem use "for /?" to get help on the for statement
set jars=d:/ProgramFiles/Java/jdk1.5.0_11/lib/tools.jar;conditions

for /f "tokens=*" %%j in ('dir /b lib\*.jar') do set jars=%jars%;%%j
echo %jars%

rem bat for mark's box, adjust as needed for yours to include the jdk tools.jar
java -cp ".;lib/appwrap-4.0.jar;lib/jetty-6.1.7.jar;lib/jetty-util-6.1.7.jar;lib/log4j-1.2.12.jar;lib/servlet-api-2.5-6.1.7.jar;lib/jsp-2.1-6.1.7.jar;lib/jsp-api-2.1-6.1.7.jar;lib/ant-1.6.5.jar;lib/amserver-8.0.jar;lib/sharedlib-8.0.jar;lib/plugins-1.0.1.jar;d:/ProgramFiles/Java/jdk1.5.0_11/lib/tools.jar;conditions" org.lds.sso.appwrap.Service %1

rem UNCOMMENT BELOW AND COMMENT ABOVE TO ATTACH DEBUGGER
rem java -Xdebug -Xrunjdwp:transport=dt_socket,address=127.0.0.1:1502,suspend=y,server=y -cp ".;lib/appwrap-4.0.jar;lib/jetty-6.1.7.jar;lib/jetty-util-6.1.7.jar;lib/log4j-1.2.12.jar;lib/servlet-api-2.5-6.1.7.jar;lib/jsp-2.1-6.1.7.jar;lib/jsp-api-2.1-6.1.7.jar;lib/ant-1.6.5.jar;lib/amserver-8.0.jar;lib/sharedlib-8.0.jar;lib/plugins-1.0.1.jar;d:/ProgramFiles/Java/jdk1.5.0_11/lib/tools.jar;conditions" org.lds.sso.appwrap.Service %1

:end