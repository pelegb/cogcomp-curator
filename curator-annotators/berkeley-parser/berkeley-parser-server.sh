#!/bin/sh
START=$PWD
DIRNAME=`dirname "$0"`
SCRIPTS_HOME=`cd "$DIRNAME" > /dev/null && pwd`
CURATOR_BASE=$SCRIPTS_HOME/..
CURATOR_BASE=`cd "$CURATOR_BASE" > /dev/null && pwd`
LIBDIR=$CURATOR_BASE/lib
COMPONENTDIR=$CURATOR_BASE/components

COMPONENT_CLASSPATH=$COMPONENTDIR/berkeley-parser-server.jar:$COMPONENTDIR/curator-interfaces.jar:$COMPONENTDIR/illinois-abstract-server.jar

LIB_CLASSPATH=$LIBDIR/ModifiedBerkeleyParser.jar:$LIBDIR/commons-configuration-1.6.jar:$LIBDIR/LBJ2Library.jar:$LIBDIR/LBJPOS.jar:$LIBDIR/LBJChunk.jar:$LIBDIR/commons-cli-1.2.jar:$LIBDIR/commons-lang-2.5.jar:$LIBDIR/commons-logging-1.1.1.jar::$LIBDIR/h2-1.1.118.jar:$LIBDIR/libthrift.jar:$LIBDIR/logback-classic-0.9.17.jar:$LIBDIR/logback-core-0.9.17.jar:$LIBDIR/slf4j-api-1.5.8.jar:$LIBDIR/commons-collections-3.2.1.jar


CLASSPATH=$COMPONENT_CLASSPATH:$LIB_CLASSPATH

cd $CURATOR_BASE
echo java -cp $CLASSPATH -Dhome=$CURATOR_BASE -Xmx2g edu.illinois.cs.cogcomp.annotation.server.BerkeleyParserServer $@
exec java -cp $CLASSPATH -Dhome=$CURATOR_BASE -Xmx2g edu.illinois.cs.cogcomp.annotation.server.BerkeleyParserServer $@
cd $START
