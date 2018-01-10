#!/bin/sh
START=$PWD
DIRNAME=`dirname "$0"`
SCRIPTS_HOME=`cd "$DIRNAME" > /dev/null && pwd`
CURATOR_BASE=$SCRIPTS_HOME/..
CURATOR_BASE=`cd "$CURATOR_BASE" > /dev/null && pwd`
LIBDIR=$CURATOR_BASE/lib
COMPONENTDIR=$CURATOR_BASE/components

COMPONENT_CLASSPATH=$CURATOR_BASE:$COMPONENTDIR/illinois-tokenizer-server.jar:$COMPONENTDIR/curator-interfaces.jar:$CURATOR_BASE:$COMPONENTDIR/illinois-abstract-server.jar

LIB_CLASSPATH=$LIBDIR/LBJava-1.0.jar:$LIBDIR/commons-cli-1.2.jar:/commons-lang-2.5.jar:$LIBDIR/commons-logging-1.1.1.jar::$LIBDIR/h2-1.1.118.jar:$LIBDIR/libthrift.jar:$LIBDIR/logback-classic-0.9.17.jar:$LIBDIR/logback-core-0.9.17.jar:$LIBDIR/slf4j-api-1.5.8.jar

CLASSPATH=$COMPONENT_CLASSPATH:$LIB_CLASSPATH

cd $CURATOR_BASE
echo java -cp $CLASSPATH -Dhome=$CURATOR_BASE -Xmx256m edu.illinois.cs.cogcomp.annotation.server.IllinoisTokenizerServer $@
exec java -cp $CLASSPATH -Dhome=$CURATOR_BASE -Xmx256m edu.illinois.cs.cogcomp.annotation.server.IllinoisTokenizerServer $@
cd $START
