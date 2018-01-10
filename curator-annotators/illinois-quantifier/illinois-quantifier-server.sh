#!/bin/sh
START=$PWD
DIRNAME=`dirname "$0"`
SCRIPTS_HOME=`cd "$DIRNAME" > /dev/null && pwd`
CURATOR_BASE=$SCRIPTS_HOME/..
CURATOR_BASE=`cd "$CURATOR_BASE" > /dev/null && pwd`
LIBDIR=$CURATOR_BASE/lib
COMPONENTDIR=$CURATOR_BASE/components
APPJAR=$LIBDIR/illinois-quantifier-1.0.0.jar

COMPONENT_CLASSPATH=$CURATOR_BASE:$COMPONENTDIR/illinois-quantifier-server.jar:$LIBDIR/curator-interfaces-0.7.jar:$CURATOR_BASE:$COMPONENTDIR/illinois-abstract-server.jar:$LIBDIR/libthrift.jar:$APPJAR:

LIB_CLASSPATH=$LIBDIR/LBJ-2.8.2.jar:$LIBDIR/LBJLibrary-2.8.2.jar:$LIBDIR/commons-cli-1.2.jar:/commons-lang-2.5.jar:$LIBDIR/commons-logging-1.1.1.jar:$LIBDIR/logback-classic-0.9.17.jar:$LIBDIR/logback-core-0.9.17.jar:$LIBDIR/slf4j-api-1.5.8.jar:$LIBDIR/illinois-pos-1.7.1.jar:$LIBDIR/edison-0.5.jar:$LIBDIR/coreUtilities-0.1.7.jar

CLASSPATH=$COMPONENT_CLASSPATH:$LIB_CLASSPATH

cd $CURATOR_BASE
echo java -cp $CLASSPATH -Dhome=$CURATOR_BASE -Xmx1G edu.illinois.cs.cogcomp.annotation.server.IllinoisQuantServer $@
exec java -cp $CLASSPATH -Dhome=$CURATOR_BASE -Xmx1G edu.illinois.cs.cogcomp.annotation.server.IllinoisQuantServer $@
cd $START
