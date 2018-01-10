#!/bin/bash

###
# starts NE Gazetteer server
#
#

source curatorVars.sh


if [ $# -eq 2 ]; then
    PORT="$1"
    CONFIG="$2"
else 
   echo "ERROR: USAGE: $0 MAIN_PORT CONFIG."
   exit 1
fi


NER_CONFIG="$CURATOR_HOME/dist/configs/NeGazetteerHandlerConfig.xml"
$CURATOR_HOME/dist/bin/illinois-ne-gazetteer-server.sh -p $PORT -c $NER_CONFIG >& $LOG_HOME/listBasedNer.$HOSTNAME.$PORT.log &


  
