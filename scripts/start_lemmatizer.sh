#!/bin/bash

###
# starts Curator lemmatizer component server
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

LOG=$LOG_HOME/illinois-lemmatizer.$HOSTNAME.$PORT.log 

CONFIG="$CURATOR_HOME/dist/configs/lemmatizerConfig.txt"

CMD="$CURATOR_HOME/dist/bin/illinois-lemmatizer-server.sh -p $PORT -c $CONFIG"

echo "$0: running command '$CMD'"

nice $CMD >& $LOG &


chmod g+rw $LOG
