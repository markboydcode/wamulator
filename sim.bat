set CONFIG_FILE="community_logins.xml"
java -cp "appwrap-1.4.jar;lib/jetty-6.1.7.jar;lib/jetty-util-6.1.7.jar;lib/log4j-1.2.12.jar;lib/servlet-api-2.5-6.1.7.jar;lib/jsp-2.1-6.1.7.jar;lib/jsp-api-2.1-6.1.7.jar;lib/ant-1.6.5.jar;%JAVA_HOME%/lib/tools.jar" org.lds.sso.appwrap.Service %CONFIG_FILE%
rem
rem
rem UNCOMMENT BELOW AND COMMENT ABOVE TO ATTACH DEBUGGER
rem java -Xdebug -Xrunjdwp:transport=dt_socket,address=127.0.0.1:1502,suspend=y,server=y -cp "appwrap-1.4.jar;lib/jetty-6.1.7.jar;lib/jetty-util-6.1.7.jar;lib/log4j-1.2.12.jar;lib/servlet-api-2.5-6.1.7.jar;lib/jsp-2.1-6.1.7.jar;lib/jsp-api-2.1-6.1.7.jar;lib/ant-1.6.5.jar;%JAVA_HOME%/lib/tools.jar" org.lds.sso.appwrap.Service %CONFIG_FILE%
