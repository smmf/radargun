#!/bin/bash

for n in $(cat all_machines)
do
    ssh $n "echo \"Success on node $n\"" | grep -v "Success"
done

