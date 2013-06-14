#!/bin/bash

TAR_FILE=
function absname() {
  pushd `dirname "$0"` > /dev/null
  echo `pwd`
  popd > /dev/null
}

THIS_SCRIPT_DIR=`absname`
STATS_FILE="$THIS_SCRIPT_DIR/stats-w20-ts10000-tx2-compare-ff2-vs-noff2.csv"
BENCHS=`ls $THIS_SCRIPT_DIR/10*.xml $THIS_SCRIPT_DIR/12*.xml`

if [ "x$RADARGUN_HOME" == "x" ]; then
    echo "Please set RADARGUN_HOME before executing this script (e.g. .../target/distribution/RadarGun-<version>";
    exit
fi

if [ "x$RADARGUN_MY_STUFF" == "x" ]; then
    echo "Please set RADARGUN_MY_STUFF before executing this script";
    exit
fi

if [ -f $STATS_FILE ]; then
    mv $STATS_FILE $STATS_FILE.`stat -t"%Y%m%d%H%M%S" -f "%SB" $STATS_FILE`
fi

cd "$RADARGUN_HOME"

for benchmark in $BENCHS; do
    echo "running "$benchmark
    bin/local.sh -c $benchmark
    $RADARGUN_MY_STUFF/bin/rg-merge-stats.sh $RADARGUN_HOME/reports/local_benchmark.csv $STATS_FILE
done

echo "Done. All data in $STATS_FILE"

