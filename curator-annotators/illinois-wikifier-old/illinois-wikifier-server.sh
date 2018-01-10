#!/bin/sh
START=$PWD
DIRNAME=`dirname "$0"`
SCRIPTS_HOME=`cd "$DIRNAME" > /dev/null && pwd`
CURATOR_BASE=$SCRIPTS_HOME/..
CURATOR_BASE=`cd "$CURATOR_BASE" > /dev/null && pwd`
LIBDIR=$CURATOR_BASE/lib
COMPONENTDIR=$CURATOR_BASE/components

COMPONENT_CLASSPATH=$CURATOR_BASE:$COMPONENTDIR/illinois-wikifier-server.jar:$COMPONENTDIR/curator-interfaces.jar:$CURATOR_BASE:$COMPONENTDIR/illinois-abstract-server.jar

LIB_CLASSPATH=$LIBDIR/LBJLibrary-2.8.2.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/commons-cli-1.2.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/commons-lang-2.5.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/commons-logging-1.1.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/logback-classic-0.9.28.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/logback-core-0.9.28.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/slf4j-api-1.6.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/h2-1.1.118.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/hadoop-0.17.0-core.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/lucene-core-2.4.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/protobuf-java-2.3.0.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/jwnl-1.4_rc3.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/log4j-1.2.13.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/secondstring-20060615.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/re.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/edison-0.2.9.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/coreUtilities-0.1.4.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/liblinear-1.5-with-deps.jar 
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/libthrift.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/Wikifier.jar

CLASSPATH=$COMPONENT_CLASSPATH:$LIB_CLASSPATH

cd $CURATOR_BASE

CMD="java -cp $CLASSPATH -Dhome=$CURATOR_BASE -Xmx6000m edu.illinois.cs.cogcomp.annotation.server.ReferenceAssistantServer -c configs/Demo_Config_Deployed.txt $@"
echo "$0: running command '$CMD'..."

exec $CMD

cd $START