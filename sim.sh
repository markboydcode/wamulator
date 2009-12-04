#!/bin/sh
# -----------------------------------------------------------------------------
# Start Script for the SSO Header Proxy
# -----------------------------------------------------------------------------
#
# You need to set JAVA_HOME in your environment and have $JAVA_HOME/bin in your PATH
#
#
java -cp ./appwrap-1.4.jar:./lib/jetty-6.1.7.jar:./lib/jetty-util-6.1.7.jar:./lib/log4j-1.2.12.jar:./lib/servlet-api-2.5-6.1.7.jar:./lib/jsp-2.1-6.1.7.jar:./lib/jsp-api-2.1-6.1.7.jar:./lib/ant-1.6.5.jar:"$JAVA_HOME"/lib/tools.jar org.lds.sso.appwrap.Service ./community_logins.xml
