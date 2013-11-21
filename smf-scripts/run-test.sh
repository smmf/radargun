#!/bin/bash

WORKING_DIR=`cd $(dirname $0); pwd`

echo "loading environment..."
. ${WORKING_DIR}/environment.sh


# $1: should be the plugin's name, e.g. hazelcast3
# $2: should be the plugin's template config, e.g. repl-sync-template.xml ("-template" in the filename will be removed to create the final file!)
# $3: should be the number of nodes
function fixPluginConf() {
    P_TEMPLATE="${RADARGUN_DIR}/plugins/${1}/conf/${2}"
    P_FINAL="${RADARGUN_DIR}/plugins/${1}/conf/${2/-template/}"
    sed -e s/NODE_SIZE_TEMPLATE/$3/g "${P_TEMPLATE}" > "${P_FINAL}"
}



NUM_ENTRIES=$1
WRITE_PERCENTAGE=$2
WRITE_TX_PERCENTAGE=$3
PRODUCT=$4

NR_NODES_TO_USE=`wc -l /home/ubuntu/machines | awk '{print $1}'`
TEST_DURATION="1"

BENC_DEFAULT="-n ${NR_NODES_TO_USE} -ne ${NUM_ENTRIES} -wp ${WRITE_PERCENTAGE} -wtp ${WRITE_TX_PERCENTAGE} -p ${PRODUCT}"

echo "============ INIT BENCHMARKING ==============="

clean_master
# kill_java ${CLUSTER}
# clean_slaves ${CLUSTER}

echo "============ STARTING BENCHMARKING ==============="

#lp => locality probability= 0 15 50 75 100
#wrtPx => write percentage== 0 10
#rdg => replication degree== 1 2 3

#${JGRP_GEN} -sequencer -toa -tcp
#cp ${RADARGUN_DIR}/plugins/infinispan4/bin/jgroups/jgroups.xml ${WORKING_DIR}/conf/jgroups/jgroups.xml


echo "After generating jgroups"

for owner in 1; do
#for l1 in -l1-rehash none; do
#for wrtPx in 0 10; do
#for rdg in 1 2 3; do
#for keys in 1000 8000 16000; do
#for bfFp in 0.01 0.10; do

#${ISPN_GEN} ${ISPN_DEFAULT} -num-owner ${owner}
echo "============= Replace plugin templates with NR_NODES_TO_USE=${NR_NODES_TO_USE} ==========="
# Run a fix for known plugin configuration templates (hardwired list here! one invocation per line!)
fixPluginConf hazelcast3 hzl3-repl-sync-template.xml ${NR_NODES_TO_USE}
fixPluginConf ff2 rg-fenix-framework-config-options-template.properties ${NR_NODES_TO_USE}
fixPluginConf ff2-lf rg-fenix-framework-config-options-template.properties ${NR_NODES_TO_USE}

echo "============= Going to generate benchmark ==========="
${BENC_GEN} ${BENC_DEFAULT}
echo "=============== Going to run TEST ================"
run_test ${NR_NODES_TO_USE} "results2" ${TEST_DURATION} ${CLUSTER}
killall -9 java
done
#done
#done

echo "============ FINISHED BENCHMARKING ==============="

exit 0
