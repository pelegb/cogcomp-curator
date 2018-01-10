#!/bin/bash

PORT=

if [ $# -eq 1 ]; then
   PORT="$1"
else
   echo "Usage: $0 PORT"
   exit 1
fi

source curatorVars.sh


LOG="$LOG_HOME/illinois_coref.$HOSTNAME.$PORT.log"
$CURATOR_HOME/dist/bin/illinois-coref-server.sh -p $PORT >& $LOG &

chmod g+rw $LOG

