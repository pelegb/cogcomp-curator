#!/bin/bash

###
# starts Wikifier server
#
#

source curatorVars.sh

if [ $# -eq 1 ]; then
   PORT="$1"
else
   echo "Usage: $0 PORT"
   exit 1
fi

LOG=$LOG_HOME/wikifier.$HOSTNAME.$PORT.log

echo "Writing to the log file: $LOG"

CONFIG="$CURATOR_HOME/dist/configs/Demo_Config_Deployed.txt"

echo $CONFIG 

CMD="nohup nice $CURATOR_HOME/dist/bin/illinois-wikifier-server.sh --port $PORT --config $CONFIG"

echo "running command: $CMD >& $LOG &"

$CMD >& $LOG & 


chmod g+rw $LOG

  
