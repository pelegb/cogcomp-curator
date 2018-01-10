#!/bin/bash

#This is the script to start mongo server
#mongo="/shared/gargamel/mongodb-linux-x86_64-2.0.6/bin"

## NOTE: added "-v" option to help debug update problem

mkdir -p $MONGO_ARCHIVE
mkdir -p $MONGO_LOG

echo Starting Mongod Server ...
exec nohup nice ${MONGO_BIN}/mongod -v --fork --logpath ${MONGO_LOG}/mongod.log --logappend --dbpath ${MONGO_ARCHIVE}

echo "$0: Mongod server started. Log File Placed in ${MONGO_LOG}/mongod.log."
