#!/bin/bash

PORT=

if [ $# -eq 1 ]; then
   PORT="$1"
else
   echo "Usage: $0 PORT"
   exit 1
fi

source curatorVars.sh

LOG=$LOG_HOME/curator.$HOSTNAME.$PORT.log

CURATOR_CONFIG=configs/curator.properties
ANNOTATORS_CONFIG=configs/annotators-trollope.xml

nohup nice $CURATOR_START_DIR/bin/curator.sh --port $PORT --threads 10 --config $CURATOR_CONFIG --annotators $ANNOTATORS_CONFIG >& $LOG &

chmod g+rw $LOG
