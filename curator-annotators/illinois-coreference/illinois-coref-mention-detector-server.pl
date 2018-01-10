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

my $COMPONENT_CLASSPATH="$CURATOR_BASE:$COMPONENTDIR/illinois-coreference-server.jar:$COMPONENTDIR/curator-interfaces.jar";

my $RUNIT = "$DIRNAME/runIt.pl";
my $MAIN_CLASS="edu.illinois.cs.cogcomp.annotation.server.IllinoisCorefMentionDetectorServer";

print STDERR "$0: main class is $MAIN_CLASS.\n";

my $KCWORKSPACE = "/shared/experiments/kchang10/workspace";

my $KCCOREF = "$KCWORKSPACE/112-latentcoref";


my $COREFCONFIG = "configs";
my $DATA = "data/coref";


my $LIB_CLASSPATH ="$LIBDIR/CoNLLCoref-1.0.jar";
$LIB_CLASSPATH .= ":$COREFCONFIG";
$LIB_CLASSPATH .= ":$DATA";

$LIB_CLASSPATH .=":$LIBDIR/IllinoisCoreferenceResources-2.0.jar";
$LIB_CLASSPATH .= ":$LIBDIR/illinoisCorefOntonotesModel-1.0-CoNLL.jar";
$LIB_CLASSPATH .= ":$KCWORKSPACE/StanfordCoref";
$LIB_CLASSPATH .= ":$KCWORKSPACE/StanfordCoref/bin";
$LIB_CLASSPATH .= ":$KCCOREF/countTable";

$LIB_CLASSPATH .= ":$KCCOREF/src/main/resources";
#$LIB_CLASSPATH .= ":$KCCOREF/src/main/resources/config";

$LIB_CLASSPATH .= ":$KCCOREF/target/dependency";
$LIB_CLASSPATH .= ":$KCCOREF/target/dependency/*";



$LIB_CLASSPATH .= ":$LIBDIR/CorefUtils-0.0.1.jar";
$LIB_CLASSPATH .= ":$LIBDIR/MSUtils-0.2-SNAPSHOT.jar";
$LIB_CLASSPATH .= ":$LIBDIR/LBJ2Library-2.8.2.jar";
$LIB_CLASSPATH .= ":$LIBDIR/LBJ2-2.8.2.jar";
$LIB_CLASSPATH .= ":$LIBDIR/edison-0.2.9.jar";
$LIB_CLASSPATH .= ":$LIBDIR/CorefConstraints-0.1.25.jar";
$LIB_CLASSPATH .= ":$LIBDIR/coreUtilities-0.1.2.jar";
#$LIB_CLASSPATH .= ":$LIBDIR/curator-client-0.6.jar";
$LIB_CLASSPATH .= ":$LIBDIR/EntityComparison.jar";
$LIB_CLASSPATH .= ":$LIBDIR/LBJPOS-1.7.1.jar";
$LIB_CLASSPATH .= ":$LIBDIR/junit-4.1.jar";
$LIB_CLASSPATH .= ":$LIBDIR/jwnl-1.4_rc3.jar";
$LIB_CLASSPATH .= ":$LIBDIR/liblinear-1.5.1.jar";
$LIB_CLASSPATH .= ":$LIBDIR/libthrift.jar";
$LIB_CLASSPATH .= ":$LIBDIR/logback-classic-0.9.28.jar";
$LIB_CLASSPATH .= ":$LIBDIR/logback-core-0.9.28.jar";
$LIB_CLASSPATH .= ":$LIBDIR/MSUtils-0.1.12.jar";
$LIB_CLASSPATH .= ":$LIBDIR/SecondString.jar";
$LIB_CLASSPATH .= ":$LIBDIR/slf4j-api-1.6.1.jar";
$LIB_CLASSPATH .= ":$LIBDIR/snowball-1.0.jar";
#$LIB_CLASSPATH .= ":$LIBDIR/ace_model.jar";
#$LIB_CLASSPATH .= ":$LIBDIR/ontonote_model.jar";
$LIB_CLASSPATH .= ":$LIBDIR/commons-cli-1.2.jar";
$LIB_CLASSPATH .= ":$LIBDIR/commons-lang-2.5.jar";
$LIB_CLASSPATH .= ":$LIBDIR/commons-logging-1.1.1.jar";

$LIB_CLASSPATH .= ":$KCCOREF";



my $CLASSPATH="$COMPONENT_CLASSPATH:$LIB_CLASSPATH";

print STDERR "ARGS are: @ARGV\n";

croak "Usage: $0 IDENTIFIER PORT CONFIG" unless @ARGV == 3;


my $ID = shift @ARGV; 
my $PORT = shift @ARGV;
my $CONFIG = shift @ARGV;

my $CMD="$RUNIT java -cp $CLASSPATH -Dhome=$CURATOR_BASE -Xmx2G $MAIN_CLASS -p $PORT -c $CONFIG $ID";

system( "cd $CURATOR_BASE" );
print STDERR "running command: $CMD";

exec $CMD;

system( "cd $START" );
