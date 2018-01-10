#!/bin/bash

###
# starts NE Chunker server
#
#


PORT=

if [ $# -eq 1 ]; then
   PORT="$1"
else
   echo "Usage: $0 PORT"
   exit 1
fi

source curatorVars.sh

LOG=$LOG_HOME/illinois-chunk.$HOSTNAME.$PORT.log 


CMD="$CURATOR_HOME/dist/bin/illinois-chunker-server.sh -p $PORT"

echo "$0: running command '$CMD'"

nice $CMD >& $LOG &


chmod g+rw $LOG
