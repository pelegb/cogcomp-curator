#!/usr/bin/perl

use strict;
use Carp;

my $START=`pwd`;
chomp $START;

my $DIRNAME=`dirname "$0"`;
chomp $DIRNAME;
print STDERR "DIRNAME: $DIRNAME\n";

chdir $DIRNAME;

my $SCRIPTS_HOME=`pwd`;
chomp $SCRIPTS_HOME;

print STDERR  "SCRIPTS_HOME: $SCRIPTS_HOME\n";
chdir "..";

my $CURATOR_BASE=`pwd`;
chomp $CURATOR_BASE;

#$CURATOR_BASE=`cd "$CURATOR_BASE" > /dev/null && pwd`;
my $LIBDIR="$CURATOR_BASE/lib";
my $COMPONENTDIR="$CURATOR_BASE/components";

my $COMPONENT_CLASSPATH="$CURATOR_BASE:$COMPONENTDIR/illinois-abstract-server.jar";

my $RUNIT = "$DIRNAME/runIt.pl";
#my $MAIN_JAR="IllinoisNerExtended-2.8-SNAPSHOT.jar";
my $MAIN_JAR="illinois-ner-2.8.2.jar";
my $CONLL_MODEL_JAR="illinois-ner-2.6-models-conll.jar";
my $ONTO_MODEL_JAR="illinois-ner-2.6-models-ontonotes.jar";
my $MAIN_CLASS="edu.illinois.cs.cogcomp.annotation.server.IllinoisNerExtServer";

print STDERR "$0: main jar is $MAIN_JAR.\n";

my $LIB_CLASSPATH="$LIBDIR/$MAIN_JAR";
$LIB_CLASSPATH.=":$LIBDIR/$CONLL_MODEL_JAR";
$LIB_CLASSPATH.=":$LIBDIR/$ONTO_MODEL_JAR";
$LIB_CLASSPATH.=":$LIBDIR/curator-interfaces-0.7.jar";
$LIB_CLASSPATH.=":$LIBDIR/libthrift-0.8.0.jar";
$LIB_CLASSPATH.=":$LIBDIR/LBJava-1.0.jar";
$LIB_CLASSPATH.=":$LIBDIR/commons-cli-1.2.jar";
$LIB_CLASSPATH.=":$LIBDIR/commons-lang-2.5.jar";
$LIB_CLASSPATH.=":$LIBDIR/commons-logging-1.1.1.jar";
$LIB_CLASSPATH.=":$LIBDIR/coreUtilities-0.2.8.jar";
$LIB_CLASSPATH.=":$LIBDIR/libthrift.jar";
#$LIB_CLASSPATH.=":$LIBDIR/logback-classic-0.9.17.jar";
#$LIB_CLASSPATH.=":$LIBDIR/logback-core-0.9.17.jar";
$LIB_CLASSPATH.=":$LIBDIR/slf4j-api-1.5.8.jar";
$LIB_CLASSPATH.=":$LIBDIR/logback-classic-0.9.17.jar";
$LIB_CLASSPATH.=":$LIBDIR/logback-core-0.9.17.jar";
$LIB_CLASSPATH.=":$LIBDIR/commons-configuration-1.6.jar";
$LIB_CLASSPATH.=":$LIBDIR/lucene-core-2.4.1.jar";
$LIB_CLASSPATH.=":$LIBDIR/curator-utils-0.0.4-SNAPSHOT.jar";
$LIB_CLASSPATH.=":$LIBDIR/httpclient-4.1.2.jar";
$LIB_CLASSPATH.=":$LIBDIR/httpcore-4.1.3.jar";
$LIB_CLASSPATH.=":$LIBDIR/illinois-common-resources-1.1.jar";
$LIB_CLASSPATH.=":$LIBDIR/java-cup-0.11a.jar";
$LIB_CLASSPATH.=":$LIBDIR/liblinear-1.94.jar";
$LIB_CLASSPATH.=":$LIBDIR/trove4j-3.0.3.jar";
$LIB_CLASSPATH.=":$LIBDIR/weka-stable-3.6.10.jar";

print STDERR "## $0: LIB_CLASSPATH is '$LIB_CLASSPATH'...\n";

my $CLASSPATH="$COMPONENT_CLASSPATH:$LIB_CLASSPATH";

print STDERR "ARGS are: @ARGV\n";

croak "Usage: $0 IDENTIFIER PORT CONFIG" unless @ARGV == 3;


my $ID = shift @ARGV; 
my $PORT = shift @ARGV;
my $CONFIG = shift @ARGV;

my $CMD="$RUNIT java -cp $CLASSPATH -Dhome=$CURATOR_BASE -Xmx8G $MAIN_CLASS -p $PORT -c $CONFIG $ID";

system( "cd $CURATOR_BASE" );
print STDERR "running command: $CMD";

exec $CMD;

system( "cd $START" );
