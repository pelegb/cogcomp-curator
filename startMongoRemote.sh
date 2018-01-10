#!/bin/bash

#This is the script to start mongo server
#mongo="/shared/gargamel/mongodb-linux-x86_64-2.0.6/bin"

source setEnvVars.sh


if [ ! -e $MONGO_ARCHIVE ]; then
    mkdir $MONGO_ARCHIVE
fi

if [ ! -e $MONGO_LOG ]; then
    mkdir $MONGO_LOG
fi

echo Starting Mongod Server ...

## note that '--auth' has been set, since we're creating a process listening
##   on a port -- users will have to be created from the localhost

exec nohup nice ${MONGO_BIN}/mongod  --auth --fork --logpath ${MONGO_LOG}/mongod.log --logappend --dbpath ${MONGO_ARCHIVE} --port $MONGO_PORT

echo "$0: Mongod server started. Log File Placed in ${MONGO_LOG}/mongod.log."
