#!/bin/sh
START=$PWD
DIRNAME=`dirname "$0"`
SCRIPTS_HOME=`cd "$DIRNAME" > /dev/null && pwd`
CURATOR_BASE=$SCRIPTS_HOME/..
CURATOR_BASE=`cd "$CURATOR_BASE" > /dev/null && pwd`
LIBDIR=$CURATOR_BASE/lib
COMPONENTDIR=$CURATOR_BASE/components

CONFIG=$CURATOR_BASE/configs


COMPONENT_CLASSPATH=$CURATOR_BASE:$COMPONENTDIR/illinois-lemmatizer-server.jar:$COMPONENTDIR/curator-interfaces.jar:$COMPONENTDIR/illinois-abstract-server.jar:$LIBDIR/illinois-lemmatizer-0.0.7.jar

LIB_CLASSPATH=$LIBDIR/commons-cli-1.2.jar:$LIBDIR/commons-lang-2.5.jar:$LIBDIR/commons-logging-1.1.1.jar:$LIBDIR/libthrift-0.8.0.jar:$LIBDIR/logback-classic-0.9.17.jar:$LIBDIR/logback-core-0.9.17.jar:$LIBDIR/slf4j-api-1.5.8.jar:$LIBDIR/coreUtilities-0.1.7.jar:$LIBDIR/edison-0.5.jar:$LIBDIR/lucene-snowball-3.0.3.jar:$LIBDIR/lucene-analyzers-common-4.1.0.jar:$LIBDIR/cogcomp-common-resources-1.2.jar:$LIBDIR/jwnl-1.4_rc3.jar:$LIBDIR/snowball-1.0.jar:$LIBDIR/trove4j-3.0.3.jar:$LIBDIR/jwi-2.2.3.jar:$LIBDIR/curator-utils-0.0.3.jar:$CONFIG

CLASSPATH=$COMPONENT_CLASSPATH:$LIB_CLASSPATH

cd $CURATOR_BASE
CMD="java -cp $CLASSPATH -Dhome=$CURATOR_BASE -Xmx512m edu.illinois.cs.cogcomp.annotation.server.IllinoisLemmatizerServer $@"

echo "$0: running command '$CMD'..."

exec $CMD

cd $START
