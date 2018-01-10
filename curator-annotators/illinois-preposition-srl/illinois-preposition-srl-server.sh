#!/bin/sh
START=$PWD
DIRNAME=`dirname "$0"`
SCRIPTS_HOME=`cd "$DIRNAME" > /dev/null && pwd`
CURATOR_BASE=$SCRIPTS_HOME/..
CURATOR_BASE=`cd "$CURATOR_BASE" > /dev/null && pwd`
LIBDIR=$CURATOR_BASE/lib
COMPONENTDIR=$CURATOR_BASE/components
CONFIGDIR=$CURATOR_BASE/configs

VERSION=4.1-SNAPSHOT
BIN_JAR=illinoisSRL-$VERSION.jar
MODEL_JAR=illinois-preposition-models-0.1.jar

COMPONENT_CLASSPATH=$COMPONENTDIR/illinois-preposition-srl-server.jar:$CURATOR_BASE:$COMPONENTDIR/illinois-abstract-server.jar:$COMPONENTDIR/curator-interfaces.jar

BIN_CLASSPATH=$LIBDIR/preposition-role-0.1.jar:$LIBDIR/relations-core-0.1.jar
MODEL_CLASSPATH=$LIBDIR/$MODEL_JAR

LIB_CLASSPATH=$CURATOR_BASE:$BIN_CLASSPATH:$LIBDIR/commons-cli-1.2.jar:$LIBDIR/commons-collections-3.2.1.jar:$LIBDIR/commons-configuration-1.6.jar:$LIBDIR/commons-lang-2.5.jar:$LIBDIR/commons-logging-1.1.1.jar:$LIBDIR/coreUtilities-0.1.6.jar:$LIBDIR/edison-0.5.jar:$LIBDIR/jwnl-1.4_rc3.jar:$LIBDIR/LBJLibrary-2.8.2.jar:$LIBDIR/libthrift.jar:$LIBDIR/logback-classic-0.9.28.jar:$LIBDIR/logback-core-0.9.28.jar:$LIBDIR/slf4j-api-1.6.1.jar:$LIBDIR/snowball-1.0.jar:$LIBDIR/JLIS-core-0.5.jar:$LIBDIR/JLIS-multiclass-0.5.jar:$LIBDIR/illinois-common-resources-1.1.jar:$LIBDIR/verb-nom-data-1.0.jar

LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/cogcomp-common-resources-1.2.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/trove4j-3.0.3.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/LBJ-2.8.2.jar



CLASSPATH=$COMPONENT_CLASSPATH:$LIB_CLASSPATH:$MODEL_CLASSPATH:$CONFIGDIR

cd $CURATOR_BASE

echo "LIBDIR: $LIBDIR"

CMD="java -cp $CLASSPATH -Dhome=$CURATOR_BASE -Xmx4G edu.illinois.cs.cogcomp.annotation.server.IllinoisPrepositionSRLServer -c $CONFIGDIR/prep-relations.properties  $@"


echo $CMD
exec $CMD
cd $START
