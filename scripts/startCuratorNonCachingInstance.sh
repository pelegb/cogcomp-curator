#!/bin/sh


PORT=

if [ $# -eq 1 ]; then
   PORT="$1"
else
   echo "Usage: $0 PORT"
   exit 1
fi

LOG=/shared/grandma/curator/log/curator_nonCachingInstance.$HOSTNAME.$PORT.log
mv $LOG $LOG.prev
chmod g+rw $LOG.prev


CURATOR_CONFIG=configs/curator.noncaching.properties
ANNOTATORS_CONFIG=configs/annotators-ccg-noncachinginstance.xml

nohup nice /shared/grandma/curator/dist/bin/curator.sh --port $PORT --threads 10 --config $CURATOR_CONFIG --annotators $ANNOTATORS_CONFIG >& $LOG &

chmod g+rw $LOG
