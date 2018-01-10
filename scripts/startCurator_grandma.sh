#!/bin/sh

LOG=/tmp/curator_manualStart.log
mv $LOG $LOG.prev
chmod g+rw $LOG.prev

PORT=

if [ $# -eq 1 ]; then
   PORT="$1"
else
   echo "Usage: $0 PORT"
   exit 1
fi

source curatorVars.sh

LOG=$CURATOR_HOME/log/curator_nonCachingInstance.$HOSTNAME.$PORT.log
mv $LOG $LOG.prev
chmod g+rw $LOG.prev

CURATOR_CONFIG=configs/curator.noncaching.properties
ANNOTATORS_CONFIG=configs/annotators-ccg-noncachinginstance.xml


nohup nice $CURATOR_HOME/bin/curator.sh --port $PORT --threads 10 --config $CURATOR_CONFIG --annotators $ANNOTATORS_CONFIG >& $LOG &

chmod g+rw $LOG
