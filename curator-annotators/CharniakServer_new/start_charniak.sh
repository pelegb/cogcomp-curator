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


CHARNIAK_THRIFT_BASE=.
CHARNIAK_THRIFT_HOME=parser/PARSE
CONFIG_FILE=$CHARNIAK_THRIFT_BASE/config.txt
#LOG=/tmp

CMD="nohup nice -n 5 ./$RUN_SCRIPT $CHARNIAK_THRIFT_HOME/charniakThriftServer $PORT $CONFIG_FILE"

echo "running command '$CMD >& $LOG/$IDENTIFIER.log $IDENTIFIER &'"

$CMD $IDENTIFIER &

#chmod g+rw $LOG/$IDENTIFIER.log
