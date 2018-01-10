#!/bin/bash

###
# starts charniak server
#
#

if [ $# -eq 2 ]; then
    PORT="$1"
    IDENTIFIER=$2"_"$HOSTNAME"_"$PORT
else 
   echo "ERROR: USAGE: $0 MAIN_PORT IDENTIFIER."
   exit 1
fi

shopt -s expand_aliases

RUN_SCRIPT=runIt.pl

cd /shared/grandpa/servers/theArchive/configs

CHARNIAK_THRIFT_BASE=/shared/grandpa/group_software/CharniakServer_thrift
CHARNIAK_THRIFT_HOME=$CHARNIAK_THRIFT_BASE/parser05May26fixed/PARSE
CONFIG_FILE=/$CHARNIAK_THRIFT_BASE/config_kBest_noTok.txt

CMD="nohup nice -n 5 ./$RUN_SCRIPT $CHARNIAK_THRIFT_HOME/charniakThriftServerKbest $PORT $CONFIG_FILE"

echo "$0: running command '$CMD >& $DEMO_LOG/$IDENTIFIER.log $IDENTIFIER &'..."

$CMD >& $DEMO_LOG/$IDENTIFIER.log $IDENTIFIER &
