#!/bin/bash

###
# starts NER server: new code, ONTONOTES task (many labels)
#
#


PORT=
CONFIG="configs/ner.ontonotes.config"

if [ $# -eq 1 ]; then
   PORT="$1"
else
   echo "Usage: $0 PORT"
   exit 1
fi

source curatorVars.sh

IDENTIFIER=illinois-ner-ext.$HOSTNAME.$PORT

LOG=$LOG_HOME/$IDENTIFIER.log

cd $CURATOR_START_DIR

DIR=`pwd`

echo "$0: starting from dir $DIR"

CMD="bin/illinois-ner-extended-server.pl  $IDENTIFIER  $PORT $CONFIG"

echo "running command: $CMD"

nice $CMD >& $LOG &

chmod g+rw $LOG

  
