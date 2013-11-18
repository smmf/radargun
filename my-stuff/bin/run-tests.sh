RADARGUN_REPO=${HOME}/github/radargun

# this next var must be named like this to satisfy RG's scripts
export RADARGUN_HOME=${RADARGUN_REPO}/target/distribution/RadarGun-1.1.0-SNAPSHOT
export RADARGUN_MY_STUFF=${RADARGUN_REPO}/my-stuff

export RG_JAVA_OPTS="-verbose:gc -Xmx5000M -Xms3000M -server -XX:+UseConcMarkSweepGC -XX:+AggressiveOpts -verbose:gc -XX:ParallelGCThreads=4 -XX:NewRatio=5 -XX:SurvivorRatio=2 -Dhazelcast.operation.thread.count=4"

RG_LOGFILE="${RADARGUN_HOME}/radargun.log"
RG_BENCH_TEMPLATE="${RADARGUN_HOME}/conf/run-this-template.xml"
RG_BENCH_REAL="${RADARGUN_HOME}/conf/run-this-benc.xml"

# $1: should be the plugin's name, e.g. hazelcast3
# $2: should be the plugin's template config, e.g. repl-sync-template.xml ("-template" in the filename will be removed to create the final file!)
# $3: should be the number of nodes
function fixPluginConf() {
    P_TEMPLATE="${RADARGUN_HOME}/plugins/${1}/conf/${2}"
    P_FINAL="${RADARGUN_HOME}/plugins/${1}/conf/${2/-template/}"
    sed -e s/NODE_SIZE_TEMPLATE/$3/g "${P_TEMPLATE}" > "${P_FINAL}"
}

# main starts here

unamestr=`uname`
if [[ "$unamestr" == 'Linux' ]]; then
   GNU_TAIL='tail'
elif [[ "$unamestr" == 'Darwin' ]]; then
   GNU_TAIL='gtail'
fi

while getopts :c:m:n:ri:dp: opt; do
    case "$opt" in
	c)  # the test to run
            BENCH_FILE="${OPTARG}"
	    echo "Using benchmark: ${BENCH_FILE}"
	    ;;
	m)  # min nodes
	    MIN_NODES=${OPTARG}
	    echo "Starting with ${MIN_NODES} node(s)."
	    ;;
	n)  # max nodes
	    MAX_NODES=${OPTARG}
	    echo "Using up to ${MAX_NODES} node(s)."
	    ;;
	i)  # the increment
	    INCREMENT=${OPTARG}
	    echo "Stepping ${INCREMENT} node(s) each time."
	    ;;
	p)  # the FULL PATH to the single product to run
	    SINGLE_PRODUCT=${OPTARG}
	    echo "Running only product ${PRODUCT}."
	    ;;
	d)  # use this to delete data from previous runs
	    DELETE_PREVIOUS_DATA="true"
	    ;;
	r)  # use this to rebuild. untested
	    REBUILD="true"
	    ;;
	\?)
	    echo "Invalid option: -$OPTARG" >&2
	    exit
	    ;;
	:)
	    echo "Option -$OPTARG requires an argument." >&2
	    exit 1
	    ;;
    esac
done

if [ x"${REBUILD}" == x"true" ]; then
    echo "rebuilding everything"
    cd ${RADARGUN_REPO}
    \mvn clean install
fi

if [ x"${DELETE_PREVIOUS_DATA}" == x"true" ]; then
    echo "deleting data from previous runs"
    cd ${RADARGUN_HOME} && \rm -rf reports/ *.out *.log || exit
fi

#check required parameters
if [ x"${BENCH_FILE}" == x"" ]; then
    echo "Please provide the benchmark to use with -c"
    exit
fi

if [ x"${MIN_NODES}" == x"" ]; then
    echo "Please provide the minimum number of nodes to test with -m"
    exit
fi

if [ x"${MAX_NODES}" == x"" ]; then
    echo "Please provide the maximum number of nodes to test with -n"
    exit
fi

if [ x"${INCREMENT}" == x"" ]; then
    echo "Defaulting to increment 1 node at a time (change with -i)"
    INCREMENT=1
fi

#cd ${RADARGUN_REPO}
#mci

# copy my configs
\cp -r ${RADARGUN_MY_STUFF}/benchmarks/*.xml ${RADARGUN_HOME}/conf/
mkdir ${RADARGUN_HOME}/conf/products 2> /dev/null
\cp -r ${RADARGUN_MY_STUFF}/benchmarks/products/*.xml ${RADARGUN_HOME}/conf/products
\cp -r ${RADARGUN_MY_STUFF}/plugin-confs/* ${RADARGUN_HOME}/plugins/
\cp -r ${RADARGUN_MY_STUFF}/zmq-sequencer ${RADARGUN_HOME}

# check the benchmark's partial xml
if [ ! -f "${RADARGUN_HOME}/conf/${BENCH_FILE}" ]; then
    echo "The benchmarkfile does not exist!"
    exit
fi
cd ${RADARGUN_HOME}

# decide whether to run all products or just one
if [ x"${SINGLE_PRODUCT}" == x"" ]; then
    PRODUCTS=`ls ${RADARGUN_HOME}/conf/products/*.xml`
else
    EXPECTED_SINGLE_PRODUCT="${RADARGUN_HOME}/conf/products/${SINGLE_PRODUCT}"
    if [ ! -f "${EXPECTED_SINGLE_PRODUCT}" ]; then
	echo "The product to benchmark does not exist! (expected in ${EXPECTED_SINGLE_PRODUCT})"
	exit
    fi
    PRODUCTS=${EXPECTED_SINGLE_PRODUCT}
fi

for product in ${PRODUCTS}; do
    echo "RUN-TESTS: Using ${product}"

    # produce the real bench-file
    cat "conf/BENCH_PREFIX.xml" "conf/${BENCH_FILE}" "${product}" "conf/BENCH_SUFFIX.xml" > "${RG_BENCH_TEMPLATE}"

    for node in `seq ${MIN_NODES} ${INCREMENT} ${MAX_NODES}`; do 
	# let things statilize from previous runs
	echo "RUN-TESTS: Waiting a little before issuing a kill..."
	sleep 3
	echo "RUN-TESTS: Sending killall java"
	killall java
	echo "RUN-TESTS: Waiting a little before issuing a kill -9..."
	sleep 1
	echo "RUN-TESTS: Sending a killall -9 java..."
	killall -9 java
	echo "RUN-TESTS: Waiting a little more..."
	sleep 1

        java -cp ${RADARGUN_HOME}/zmq-sequencer/jeromq-0.3.2-SNAPSHOT.jar:${RADARGUN_HOME}/zmq-sequencer/pubsub-bench-1.0-SNAPSHOT.jar  bench.pubsub.ZeroMQSequencer &

	echo "RUN-TESTS: "`date "+%F %H:%M:%S"`"Running $product with $node node(s)"

	# delete stale data from previous runs
	\rm -rf ObjectStore

        SHORT_PRODUCT=`basename ${product} .xml`
        # configure the # of nodes
	sed -e s/NODE_SIZE_TEMPLATE/${node}/g -e s/PUT_FILE_NAME_HERE/${SHORT_PRODUCT}/g "${RG_BENCH_TEMPLATE}" > "${RG_BENCH_REAL}"

        # print it
	head "${RG_BENCH_REAL}" | grep initSize

	# Run a fix for known plugin configuration templates (hardwired list here! one invocation per line!)
	fixPluginConf hazelcast3 hzl3-repl-sync-template.xml ${node}
	fixPluginConf ff2 rg-fenix-framework-config-options-template.properties ${node}
	fixPluginConf ff2-lf rg-fenix-framework-config-options-template.properties ${node}

        # run master
	bin/master.sh -c "${RG_BENCH_REAL}" && sleep 3

        # wait until finished (or a big timeout)
        # a huge 1h  timeout.
	sleep 3600 & timerPid=$!
	echo "Timer's PID is "$timerPid
        # wait until either timer expires or the log contains the expected message
	${GNU_TAIL} -n0 -F --pid=$timerPid ${RG_LOGFILE} 2> /dev/null | while read line 
	do
	    if echo $line | grep -q 'Master process is being shutdown'; then
    	        echo "RUN-TESTS: MASTER COMPLETED !!!"
                # stop the timer..
		kill $timerPid 2> /dev/null
	    fi
	done &
	
	slave=0
	while [ ${slave} -ne ${node} ]; do
	    echo "RUN-TESTS: start slave "${slave}
	    sleep 1 # be patient
	    bin/slave.sh
	    slave=$((slave+1))
	done

	echo "RUN-TESTS: waiting for test to end"
	wait %sleep 2> /dev/null
	echo "RUN-TESTS: "`date "+%F %H:%M:%S"`"test ended"

    done

done
echo "RUN-TESTS: everything done!"

