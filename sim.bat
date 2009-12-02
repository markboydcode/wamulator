rem USER Needs to set M2_REPO and JAVA_HOME and have %JAVA_HOME%/bin in their PATH
rem
rem
set CONFIG_FILE="%M2_REPO%/org/lds/sso/login-simulator/1.0-SNAPSHOT/community_logins.xml"
java -cp "%M2_REPO%/org/lds/sso/login-simulator/1.0-SNAPSHOT/login-simulator-1.0-SNAPSHOT.jar;%M2_REPO%/org/mortbay/jetty/jetty/6.1.7/jetty-6.1.7.jar;%M2_REPO%/org/mortbay/jetty/jetty-util/6.1.7/jetty-util-6.1.7.jar;%M2_REPO%/log4j/log4j/1.2.12/log4j-1.2.12.jar;%M2_REPO%/org/mortbay/jetty/servlet-api-2.5/6.1.7/servlet-api-2.5-6.1.7.jar;%M2_REPO%/org/mortbay/jetty/jsp-2.1/6.1.7/jsp-2.1-6.1.7.jar;%M2_REPO%/org/mortbay/jetty/jsp-api-2.1/6.1.7/jsp-api-2.1-6.1.7.jar;%M2_REPO%/ant/ant/1.6.5/ant-1.6.5.jar;%JAVA_HOME%/lib/tools.jar" org.lds.sso.appwrap.Service %CONFIG_FILE%
