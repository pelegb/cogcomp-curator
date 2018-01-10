#!/bin/bash
resourceDir=resources
configDir=config

classpath="bin/IllinoisCoref-0.1-SNAPSHOT.jar"

for JAR in `ls lib` 
do
   classpath=$classpath:"lib/$JAR"
done

for JAR in `ls model`
do
   classpath=$classpath:"model/$JAR"
done


#dataDir="data:data/ace:data/conll:/shared/grandpa/corpora/ace_tides_multling_train/"
#:${dataDir}
export CLASSPATH=${classpath}:.:${resourceDir}:${configDir}

CMD="java -Xmx25g -cp ${CLASSPATH} edu.illinois.cs.cogcomp.lbj.coref.main.MainClass $1 $2 $3 $4 $5 $6 $7 $8 $9"

echo "$0: running command '$CMD'..." 

$CMD
