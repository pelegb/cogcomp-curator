#!/bin/bash

###
# starts verb SRL server
#
#


PORT=

if [ $# -eq 2 ]; then
   PORT="$1"
   IDENTIFIER="$2"
else
   echo "Usage: $0 PORT IDENTIFIER"
   exit 1
fi

source curatorVars.sh

LOG=$LOG_HOME/illinois-verb-srl.$HOSTNAME.$PORT.log 


CMD="$CURATOR_HOME/dist/bin/illinois-verb-srl-server.sh -p $PORT $IDENTIFIER"

echo "$0: running command '$CMD'"

nice $CMD >& $LOG &


chmod g+rw $LOG
