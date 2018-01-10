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

IDENTIFIER=illinois-ner-basic.$HOSTNAME.$PORT

LOG=$LOG_HOME/$IDENTIFIER.log


#CMD="$CURATOR_HOME/dist/bin/illinois-ner-extended-server.pl  $IDENTIFIER -p $PORT -c $CONFIG"

cd $CURATOR_START_DIR

CMD="bin/illinois-ner-extended-server.pl  $IDENTIFIER  $PORT $CONFIG"

echo "running command: $CMD"


nice $CMD >& $LOG &

chmod g+rw $LOG

  
