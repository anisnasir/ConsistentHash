#!/bin/bash
JAR="target/StreamLoadBalancing-0.0.5-SNAPSHOT-jar-with-dependencies.jar"
NumberOfServers="5 10 50 100"
NumberOfReplicas="10 100 1000"
maxprocs="10"
command="java -Xms512m -Xmx4096m -jar ${JAR}"
lbname="Hashing"
output="output_logs/${lbname}/output_${data}"
for ns in $NumberOfServers ; do
	for nr in $NumberOfReplicas; do   
		echo "$command 1 ${output}_${ns}_${nr}_${lbname} ${ns} ${nr}" >> ${output}_${ns}_${nr}_${lbname}.log
      		cmdlines="$cmdlines $command 1 ${output}_${ns}_${nr}_${lbname} $ns ${nr} >> ${output}_${ns}_${nr}_${lbname}.log;"
	done
done

#echo $cmdlinesi
echo -e $cmdlines | parallel --max-procs $maxprocs
echo "Done"
