#!/bin/bash

#nova list | grep -o '[0-9]\{1,3\}\.[0-9]\{1,3\}\.[0-9]\{1,3\}\.[0-9]\{1,3\}' > all_machines

#bench[1]="RW05-1000-1p"

writeTxPercentage[1]="01"
#writeTxPercentage[2]="05"
#writeTxPercentage[3]="10"
#writeTxPercentage[4]="20"

numEntries[1]="100"
#numEntries[2]="1000"
#numEntries[3]="10000"
#numEntries[4]="100000"

writePercentage[1]="1"

#product[1]="fflf-tdmock-g2800ns-p67000ns-c64000ns"
#product[2]="tdmock-g2800ns-p67000ns-c64000ns"
product[3]="fflf-ispn53-repl-sync-rc"
product[4]="ispn53-repl-sync-rc"

nr_nodes[1]="2"
#nr_nodes[2]="4"
#nr_nodes[3]="6"
#nr_nodes[4]="8"
#nr_nodes[5]="10"
#nr_nodes[6]="12"
#nr_nodes[8]="16"

echo "copying scripts to target"
\cp -r smf-scripts/*.sh target/distribution/RadarGun-1.1.0-SNAPSHOT/bin

echo "copying product benchmarks to target"
\cp -r my-stuff/benchmarks/products target/distribution/RadarGun-1.1.0-SNAPSHOT/conf

echo "copying plugin-confs to target"
\cp -r my-stuff/plugin-confs/* target/distribution/RadarGun-1.1.0-SNAPSHOT/plugins

mv auto-results auto-results.`date "+%Y%m%d-%H%M%S"`

mkdir auto-results

for attempt in 1; do
    echo "going for attempt $attempt"

    for p in ${product[@]}; do
	echo "going for product ${p}"

	for ne in ${numEntries[@]}; do 
	    echo "going for numEntries ${ne}"

	    for wp in ${writePercentage[@]}; do
		echo "going for writePercentage ${wp}"

		for wtp in ${writeTxPercentage[@]}; do
		    echo "going for writeTxPercentage ${wtp}"
		    

		    mkdir auto-results/logs-wtp${wtp}_ne${ne}_wp${wp}_${p}

                    #for nodes in 8 16 24 32 40 48 56 64 80 100
		    for nodes in ${nr_nodes[@]}; do
			head -$nodes all_machines > /home/$USER/machines

			echo "going for nodes $nodes"

			echo "bash smf-scripts/run-test.sh ${ne} ${wp} ${wtp} ${p}"
			bash smf-scripts/run-test.sh ${ne} ${wp} ${wtp} ${p}
			mv results-radargun/test-result-results2/${p}_${nodes}.csv auto-results/wtp${wtp}_ne${ne}_wp${wp}_${p}_${nodes}.csv

			mv results-radargun/test-result-results2/logs/* auto-results/logs-wtp${wtp}_ne${ne}_wp${wp}_${p}
			rc=$?
			if [[ $rc != 0 ]] ; then
			    echo "Error within: bash smf-scripts/run-test.sh ${ne} ${wp} ${wtp} ${p} using ${nodes} nodes" >> auto-results/error.out
			fi
		    done
		    
		done
	    done
	done
    done
done

