#!/bin/bash



PORT=

if [ $# -eq 1 ]; then
  PORT="$1"
else
  echo "Usage: $0 PORT"
  exit 1
fi

source curatorVars.sh

EF_DIR=$CURATOR_HOME/non-java/easyfirst

LOG="$LOG_HOME/easyfirst.$HOSTNAME.$PORT.log"

cd $EF_DIR
./easyfirst_server.py --port $PORT >&$LOG &

chmod g+rw $LOG

