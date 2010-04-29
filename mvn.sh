#!/bin/bash

export JAVA_HOME=/usr/java/jdk1.6.0_14
export M2_HOME=/usr/maven/maven-2.2.1
export MAVEN_OPTS=-Xmx512m
export PATH=/usr/local/bin:/bin:/usr/bin:/usr/X11R6/bin:/home/bldmgr/bin:$M2_HOME/bin:$JAVA_HOME/bin:.
export DISPLAY=`cat /var/xvfb/DISPLAY` XAUTHORITY=/var/xvfb/XAUTHORITY

svn update

DATE=`date +%m%d%H%M`
REVISION=`svn info | grep Revision | awk '{ print $2 }'`
SVNURL=`svn info | grep URL | awk '{ print $2 }'`

echo "Building and gathering test results, please wait..."

mvn clean install "$@" -B \
                -DbuildDate=$DATE \
                -DbuildRevision=$REVISION \
                -DbuildUrl=$SVNURL

if [ "$?" = "0" ]; then
 		mvn deploy site-deploy "$@" -B \
                -DbuildDate=$DATE \
                -DbuildRevision=$REVISION \
                -DbuildUrl=$SVNURL \
                -Dmaven.test.skip=true
fi
