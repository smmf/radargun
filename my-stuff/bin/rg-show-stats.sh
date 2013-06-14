#!/bin/bash

SRC=`mktemp /tmp/smfXXXXX`

if [ "x$2" == "x" ]
then
    DST=`mktemp /tmp/smfXXXXX`
else
    DST="$2"
fi
#DST=`mktemp /tmp/smfXXXXX`

cp $1 $SRC

# echo "
# " >> $SRC
# echo "newline" >> $SRC
# cat $SRC

# # # echo "<table border=1>" > ${DST}
# # # cat $SRC | while read LINE; do echo "<tr><td>${LINE//,/</td><td>}</td></tr>"; done >> ${DST}
# # # echo "</table>" >> ${DST}

# cat $SRC | while read LINE; do echo "${LINE//,/}"; done


#echo -n "PRODUCT"

FILENAME=$1

echo "<table border=1>" > ${DST}
for i in `seq 0 13`
do
    echo -n "<tr>" >> ${DST}
    cat $FILENAME | while read LINE
    do

        IFS=","
        columns=( $LINE )
        if [ ${#columns[@]} -gt 0 ]; then 
#        echo "columns are "${columns[@]}
            VALUE=`printf '%.0f' ${columns[$i]} 2> /dev/null`
            NUMBER_RES=`echo $?`
            if [ $NUMBER_RES -eq 0 ]; then
                echo -n "<td>${VALUE}</td>" | tr -d ' ' >> ${DST}
            else
                echo -n "<td>${columns[$i]}</td>" | tr -d ' ' >> ${DST}
            fi
        fi
    done
    echo "</tr>" >> ${DST}

done
echo "</table>" >> ${DST}

open -a Google\ Chrome ${DST}




