#!/bin/bash




# NOTE: The following five variables are the ones you need to set based
#   on your system's configuration/where you installed Thrift, Boost, 
#   and Curator. If you see errors compiling the Charniak component,
#   one or more of these is probably incorrect. 

export THRIFT_ROOT="/scratch/thrift"
export CURATOR_HOME="/scratch/mssammon/curator"
export BOOST_ROOT="/scratch/downloads/boost_1_55_0"
export BOOST_INC_DIR="${BOOST_ROOT}/boost"
export MONGO_BIN="/scratch/mongodb/bin"
export PATH="$PATH:/$THRIFT_ROOT/bin"
export JAVA_HOME="/software/sun-jdk-1.7.0-latest-el6-x86_64/"

# These next variables are needed only if you want to start a non-local
#   instance of Mongodb. You will need to set the corresponding values,
#   'database.url' as the value  <MONGO_HOST>:<MONGO_PORT>, in the
#   configuration file curator/dist/configs/database.properties; for
#   example, "my.other.machine.edu:9010"

export MONGO_HOST="trollope.cs.illinois.edu" # need to set this also in database.properties
export MONGO_PORT="21987" # need to set this also in database.properties


# The next variables can be changed if desired: it names the location
#   of the curator database archive. The curator build.xml file
#   creates the directories named below by default; if you change
#   them, you will need to create the directories yourself.

export MONGO_ARCHIVE="${CURATOR_HOME}/dist/db-archive"
export MONGO_LOG="${CURATOR_HOME}/dist/db-log"



# The following variables should be correct if THRIFT_ROOT is
#   set correctly, though if the thrift distribution changes, they might
#   not be...

export THRIFT_CPP_INTERFACE_HOME="${CURATOR_HOME}/curator-interfaces/gen-cpp"
export THRIFT_COMPILER_ROOT="${THRIFT_ROOT}/compiler/cpp"
export THRIFT_CPP_ROOT="${THRIFT_ROOT}/lib/cpp/"
export THRIFT_LIB_DIR="${THRIFT_ROOT}/lib"
export THRIFT_CPP_INCLUDE="${THRIFT_ROOT}/include/thrift"
export LD_LIBRARY_PATH="${THRIFT_LIB_DIR}/:$LD_LIBRARY_PATH"
export PATH="${THRIFT_COMPILER_ROOT}:$PATH"

# trying to fix weird python path problem

export PATH="/usr/lib64:$PATH"

APPENDVAL=""

#if [ "$PYTHONPATH" != "" ]; then
#    APPENDVAL=":$PYTHONPATH"
#fi

export PYTHONPATH="/shared/trollope/opt/lib/lib64/python2.6/site-packages"$APPENDVAL
