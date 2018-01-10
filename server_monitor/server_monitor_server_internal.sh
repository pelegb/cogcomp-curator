#!/bin/bash

source curatorVars.sh

if [ $# -ne 2 ]; then
    echo "usage: server_monitor_server_internal.sh config port"
    exit 1
else
    config=$1
    port=$2
fi


server_monitor_log=$LOG_HOME/server_monitor.$HOSTNAME.$config.$port.log

nohup nice ./server_monitor.pl $config $port >& $server_monitor_log &
