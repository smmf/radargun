#!/bin/bash

SRC=$1
DST=$2

if [ ! -f $2 ]; then
#    echo "1"
    cp $1 $2
else
#    echo "2"
    cat $1 | tail -n 1 >> $2
fi
echo "" >> $2




