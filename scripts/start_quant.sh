#!/bin/bash

source curatorVars.sh

PORT=

if [ $# -eq 1 ]; then
   PORT="$1"
else
   echo "Usage: $0 PORT"
   exit 1
fi


LOG=$LOG_HOME/quantifier.$HOSTNAME.$PORT.log

QUANT_HOME=$CURATOR_HOME/non-java/quant

cd $QUANT_HOME

$QUANT_HOME/illinois-quant-server.py --port $PORT  >& $LOG &
chmod g+rw $LOG

