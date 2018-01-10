#!/bin/bash

###
# starts charniak server
#
#

source curatorVars.sh


if [ $# -eq 2 ]; then
    PORT="$1"
    IDENTIFIER=$2"_"$HOSTNAME"_"$PORT
else 
   echo "ERROR: USAGE: $0 MAIN_PORT IDENTIFIER."
   exit 1
fi

shopt -s expand_aliases

CONFIG=config.txt

#cd /shared/grandpa/servers/theArchive/configs

CHARNIAK_THRIFT_BASE=$CURATOR_HOME/dist/CharniakServer

cd $CHARNIAK_THRIFT_BASE

CHARNIAK_THRIFT_HOME=$CHARNIAK_THRIFT_BASE/parser05May26fixed/PARSE
CONFIG_FILE=$CHARNIAK_THRIFT_BASE/$CONFIG


LOG=$LOG_HOME/$IDENTIFIER.log

CMD="nohup nice -n 5  $SCRIPT_HOME/$RUN_SCRIPT $CHARNIAK_THRIFT_HOME/charniakThriftServer $PORT $CONFIG_FILE $IDENTIFIER"

echo "$0: running command '$CMD  >& $LOG  &'"

$CMD  >& $LOG  &

chmod g+rw $LOG