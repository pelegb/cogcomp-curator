#!/bin/bash

CHARNIAK_HOME=/shared/grandpa/group_software/CharniakServer

port=$PARSER_PORT


if [ $# -eq 1 ]; then
  port="$1"
fi

nohup nice -n 5 $CHARNIAK_HOME/charniak-server.pl $port >& /shared/grandpa/demos/data/log/charniak.$HOSTNAME.$port.log &
