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

	if [ "$?" = "0" ]; then
	   	export PORTFOLIO=Open_Community
	       	export PROD=App-Wrap-Shim
	        export VERSION=`grep -A 6 modelVersion pom.xml | grep version | awk -F\> '{ print $2 }' | awk -F\< '{ print $1 }'`

	        ssh bldmgr@10.100.100.140 "/home/bldmgr/bin/check_oclib.sh $PORTFOLIO $PROD $VERSION"            # Check for folders on OCLIB

	        echo "[INFO] *******************************************************************"
	        echo "[INFO] Copying jar files to artifacts library."
	        echo "[INFO] *******************************************************************"
	        echo "[INFO]"
	        scp target/*.jar bldmgr@10.100.100.140:/opt/LDSDev/html/artifacts/$PORTFOLIO/$PROD/$VERSION
	else
	        echo "[ERROR] *******************************************************************"
	        echo "[ERROR] Error building application."
	        echo "[ERROR] *******************************************************************"
	fi
fi




