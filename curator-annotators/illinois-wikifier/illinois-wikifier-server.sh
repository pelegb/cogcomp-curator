#!/bin/sh
START=$PWD
DIRNAME=`dirname "$0"`
SCRIPTS_HOME=`cd "$DIRNAME" > /dev/null && pwd`
CURATOR_BASE=$SCRIPTS_HOME/..
CURATOR_BASE=`cd "$CURATOR_BASE" > /dev/null && pwd`
LIBDIR=$CURATOR_BASE/lib
COMPONENTDIR=$CURATOR_BASE/components

VERSION=3.1

COMPONENT_CLASSPATH=$CURATOR_BASE:$LIBDIR/illinois-wikifier-$VERSION.jar:$COMPONENTDIR/curator-interfaces.jar:$CURATOR_BASE:$COMPONENTDIR/illinois-abstract-server.jar
LIB_CLASSPATH=$LIBDIR/bliki-core-3.0.19.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/cogcomp-common-resources-1.2.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/commons-cli-1.2.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/commons-codec-1.8.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/commons-collections-3.2.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/commons-compress-1.5.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/commons-httpclient-3.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/commons-io-2.4.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/commons-lang-2.5.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/commons-lang3-3.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/commons-logging-1.1.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/CorefConstraints-0.1.25.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/coreUtilities-0.1.7.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/edison-0.4.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/FastInfoset-1.2.12.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/glpk-java-1.0.29.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/google-api-spelling-1.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/gson-2.2.4.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/guava-14.0.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/gurobi-5.5.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/h2-1.3.157.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/hamcrest-core-1.3.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/httpclient-4.1.2.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/httpcore-4.1.3.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/illinois-abstract-server-0.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/illinois-chunker-1.5.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/illinois-coref-ace-1.5.4.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/illinois-coref-ace-model-1.5.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/illinois-coref-ace-resources-1.5.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/illinois-entity-similarity-2.0.0.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/illinois-pos-1.7.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/inference-0.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/istack-commons-runtime-2.16.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/jakarta-regexp-1.4.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/javatools-20120110.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/jaws-1.3.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/jaxb-api-2.2.9.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/jaxb-core-2.2.8-b01.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/jaxb-impl-2.2.8-b01.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/jbzip2-0.9.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/jgrapht-jdk1.5-0.7.3.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/JLIS-core-0.5.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/jsr173_api-1.0.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/junit-4.11.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/jwi-2.2.3.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/jwnl-1.4_rc3.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/LBJ-2.8.2.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/LBJLibrary-2.8.2.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/lbj-ner-tagger-1.0.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/liblinear-with-deps-1.5.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/libthrift-0.8.0.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/log4j-1.2.17.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/logback-classic-0.9.28.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/logback-core-0.9.28.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/lucene-analyzers-common-4.3.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/lucene-core-4.3.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/lucene-queries-4.3.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/lucene-queryparser-4.3.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/lucene-sandbox-4.3.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/lucene-suggest-4.3.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/mapdb-0.9.9.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/protobuf-java-2.3.0.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/SecondString-1.0.0.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/SecondString-1.0.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/slf4j-api-1.6.1.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/slf4j-log4j12-1.7.6.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/snowball-1.0.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/trove4j-3.0.3.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/wikipediaAPI-1.0.jar
LIB_CLASSPATH=$LIB_CLASSPATH:$LIBDIR/xz-1.2.jar


CLASSPATH=$COMPONENT_CLASSPATH:$LIB_CLASSPATH

cd $CURATOR_BASE
CMD="java -cp $CLASSPATH -Dhome=$CURATOR_BASE -Xmx8G edu.illinois.cs.cogcomp.annotation.server.IllinoisWikifierServer -c configs/illinois-wikifier-2014.xml $@"
echo "$0: running command '$CMD'..."

exec $CMD

