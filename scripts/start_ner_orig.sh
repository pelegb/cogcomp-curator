#!/bin/bash

###
# starts NER server
#
#


PORT=
CONFIG="configs/ner.config"

if [ $# -eq 1 ]; then
   PORT="$1"
else
   echo "Usage: $0 PORT"
   exit 1
fi

source curatorVars.sh

IDENTIFIER=illinois-ner-orig.$HOSTNAME.$PORT

LOG=$LOG_HOME/$IDENTIFIER.log

CMD="$SCRIPT_HOME/$RUN_SCRIPT $CURATOR_HOME/dist/bin/illinois-ner-server.sh -p $PORT -c $CONFIG $IDENTIFIER"

echo "running command: $CMD"


nice $CMD >& $LOG &

chmod g+rw $LOG

  
