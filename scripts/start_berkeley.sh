#!/bin/bash

###
# starts NER server: new code, CONLL task (PER/LOC/ORG/MISC)
#
#


PORT=
CONFIG="configs/ner.conll.config"

if [ $# -eq 1 ]; then
  PORT="$1"
else
   echo "Usage: $0 PORT"
   exit 1
fi

source curatorVars.sh

IDENTIFIER=berkeley-parser.$HOSTNAME.$PORT

LOG=$LOG_HOME/$IDENTIFIER.log


cd $CURATOR_START_DIR

CMD="bin/berkeley-parser-server.sh -p $PORT"

echo "$0: running command: $CMD"


nice $CMD >& $LOG &

chmod g+rw $LOG

  
