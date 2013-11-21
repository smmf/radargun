#!/bin/bash

#default values
if [ -n "${BENCH_XML_FILEPATH}" ]; then
  DEST_FILE=${BENCH_XML_FILEPATH}
else
  DEST_FILE=./benchmark.xml
fi

NODES=1
NUM_ENTRIES=1000
WRITE_PERCENTAGE=5
WRITE_TX_PERCENTAGE=1
PRODUCT=fflf-tdmock-g2800ns-p67000ns-c64000ns

CLIENTS=1
THREAD_MIGRATION=true
GHOST_READS=true
COLOCATION=true
REPLICATION_DEGREES=true
READ_ONLY_PERC=80
KEYS_SIZE=10000
KEYS_RANGE=100000
DURATION=20
LOWER_BOUND=2
INTRA_NOD_CONC=true
EMULATION="none"
WORKLOAD="X"
CACHE_CONFIG_FILE="ispn.xml"
PARTIAL_REPLICATION="false"
PASSIVE_REPLICATION="false"

help_and_exit(){
exit 0
}

while [ -n $1 ]; do
case $1 in
  -h) help_and_exit;;
  -n) NODES=$2; shift 2;;
  -ne) NUM_ENTRIES=$2; shift 2;;
  -wp) WRITE_PERCENTAGE=$2; shift 2;;
  -wtp) WRITE_TX_PERCENTAGE=$2; shift 2;;
  -p) PRODUCT=$2; shift 2;;
  -*) echo "unknown option $1"; exit 1;;
  *) break;;
esac
done

echo "Writing configuration to ${DEST_FILE}"

echo "<bench-config>" > ${DEST_FILE}

echo "   <master" >> ${DEST_FILE}
echo "         bindAddress=\"\${127.0.0.1:master.address}\"" >> ${DEST_FILE}
echo "         port=\"\${21032:master.port}\"/>" >> ${DEST_FILE}

echo "   <benchmark" >> ${DEST_FILE}
echo "         initSize=\"${NODES}\"" >> ${DEST_FILE}
echo "         maxSize=\"${NODES}\"" >> ${DEST_FILE}
echo "         increment=\"1\">" >> ${DEST_FILE}

echo "      <DestroyWrapper" >> ${DEST_FILE}
echo "            runOnAllSlaves=\"true\"/>" >> ${DEST_FILE}

echo "      <StartCluster" >> ${DEST_FILE}
echo "            staggerSlaveStartup=\"true\"" >> ${DEST_FILE}
echo "            delayAfterFirstSlaveStarts=\"5000\"" >> ${DEST_FILE}
echo "            delayBetweenStartingSlaves=\"1000\"/>" >> ${DEST_FILE}

echo "      <ClusterValidation" >> ${DEST_FILE}
echo "            partialReplication=\"${PARTIAL_REPLICATION}\"/>" >> ${DEST_FILE}

echo "      <InitStressTestWarmup entrySize=\"10\" opsCountStatusLog=\"100000\" numThreads=\"4\" transactionSize=\"50\" duration=\"60000\"/>" >> ${DEST_FILE}

echo "      <ContinueStressTestWarmup entrySize=\"10\" opsCountStatusLog=\"100000\" numThreads=\"4\" transactionSize=\"50\" duration=\"60000\"/>" >> ${DEST_FILE}
echo "      <ClearCluster/>" >> ${DEST_FILE}
echo "      <InitStressTest entrySize=\"10\" numEntries=\"${NUM_ENTRIES}\" opsCountStatusLog=\"500000\" numThreads=\"4\" useTransactions=\"true\" transactionSize=\"${NUM_ENTRIES}\" writePercentage=\"${WRITE_PERCENTAGE}\" writeTxPercentage=\"${WRITE_TX_PERCENTAGE}\" duration=\"60000\" />" >> ${DEST_FILE}
echo "      <ContinueStressTest entrySize=\"10\" numEntries=\"${NUM_ENTRIES}\" opsCountStatusLog=\"500000\" numThreads=\"4\" useTransactions=\"true\" transactionSize=\"${NUM_ENTRIES}\" writePercentage=\"${WRITE_PERCENTAGE}\" writeTxPercentage=\"${WRITE_TX_PERCENTAGE}\" duration=\"60000\" />" >> ${DEST_FILE}
echo "      <CsvReportGeneration fileName=\"${PRODUCT}\" />" >> ${DEST_FILE}

echo "   </benchmark>" >> ${DEST_FILE}

cat ${RADARGUN_DIR}/conf/products/${PRODUCT}.xml >> ${DEST_FILE}

echo "   <reports>" >> ${DEST_FILE}

echo "      <report name=\"All\" includeAll=\"true\" />" >> ${DEST_FILE}

echo "   </reports>" >> ${DEST_FILE}

echo "</bench-config>" >> ${DEST_FILE}

echo "Finished!"
