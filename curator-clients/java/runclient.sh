#!/bin/sh
START=$PWD
DIRNAME=`dirname "$0"`
CLIENT_HOME=`cd "$DIRNAME" > /dev/null && pwd`
CURATOR_BASE=$CLIENT_HOME/../..
CURATOR_BASE=`cd "$CURATOR_BASE" > /dev/null && pwd`
LIBDIR=$CURATOR_BASE/lib


COMPONENT_CLASSPATH=.:$LIBDIR/curator-interfaces-0.7.jar

LIB_CLASSPATH=$LIBDIR/libthrift.jar:$LIBDIR/logback-classic-0.9.17.jar:$LIBDIR/logback-core-0.9.17.jar:$LIBDIR/slf4j-api-1.5.8.jar

CLASSPATH=$COMPONENT_CLASSPATH:$LIB_CLASSPATH:CuratorClient.class

echo java -cp $CLASSPATH -Xmx512m CuratorClient $@
java -cp $CLASSPATH -Xmx512m CuratorClient $@
