#!/bin/bash

#ARR=("${DEFAULT_ARR[@]}"
#ARGS="${$@[@]}"
NUM=$(( $# - 1 ))
 
LOOP_INDEX=0
NEW_INDEX=0

#B=("${A[@]:1:2}")
#${#ArrayName[@]}

LAST="${#ARGS[@]}"

ARGCOPY=("${ARGS[@]:1:$LAST}")

echo "ARGS: $ARGS"
echo "LAST: $LAST"
echo "ARGCOPY: $ARGCOPY"

until [ -z $1 ]; do
    INDEX=$(( $INDEX + 1 ))
    echo "INDEX is $INDEX"
    shift

    if [ $INDEX -eq 1 ]; then
	continue
    fi

    NEW_INDEX=$(( $NEW_INDEX + 1 ))
    echo "NEW_INDEX: $NEW_INDEX"
    echo "ARITH: $((NEW_INDEX))"
    ${ARGCOPY[$NEW_INDEX]}="$1"
    echo "arrayelt: ${ARGCOPY[$(($NEW_INDEX))]}"
done

echo "ARGCOPY: $ARGCOPY"




