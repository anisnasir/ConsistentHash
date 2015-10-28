#!/bin/bash
JAR="target/StreamLoadBalancing-0.0.5-SNAPSHOT-jar-with-dependencies.jar"
NumberOfServers="5 10 50 100"
maxprocs="10"
command="java -Xms512m -Xmx4096m -jar ${JAR}"
lbname="Hashing"
output="output_logs/${lbname}/output"
for ns in $NumberOfServers ; do
	echo "$command 1 ${output}_${ns}_${lbname} ${ns} " >> ${output}_${ns}_${lbname}.log
      	cmdlines="$cmdlines $command 1 ${output}_${ns}_${lbname} $ns >> ${output}_${ns}_${lbname}.log;"
done

#echo $cmdlinesi
echo -e $cmdlines | parallel --max-procs $maxprocs
echo "Done"
