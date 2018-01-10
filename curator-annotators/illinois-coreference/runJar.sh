date
#!/bin/bash
KCWORKSPACE=/shared/experiments/kchang10/workspace
KCCOREF=${KCWORKSPACE}/112-latentcoref
COREFPACKAGE=$KCCOREF/CoNLLCoref-1.0.jar
#####
MARKLOC=/shared/grandma/mssammon/COREF/CorefConstraintExpts
MARKLIB=/shared/grandma/mssammon/lib

stanfordsys=$KCWORKSPACE/stanfordCoref:$KCWORKSPACE/stanfordCoref/bin/:$KCWORKSPACE/stanfordCoref/*

dataDir=${KCCOREF}/data:${KCCOREF}/data/v2

resourceDir=$KCCOREF/src/main/resources/:$KCCOREF/src/main/resources/config:illinoisCorefOntonotesModel-1.0-CoNLL.jar

lbjlib=/shared/grandpa/opt/src/lbj-current/build/

#lbjlib=jar:jar/*
#stanford-corenlp-2011-06-19.jar:fastutil.jar:jgraph.jar:jgrapht.jar:xom.jar:stanford-corenlp-models-2011-06-19.jar:
#export GRB_LICENSE_FILE=/home/roth/kchang10/gurobi3.lic

classpath="$COREFPACKAGE:$KCCOREF/target/dependency:$KCCOREF/target/dependency/*:$MARKLIB/MSUtils-0.1.5.jar:$MARKLOC/resources:$MARKLOC/CorefConstraints-0.1.17.jar:$MARKLIB/EntityComparison.jar:$MARKLIB/SecondString.jar:$MARKLOC/config:$MARKLOC/data"
export CLASSPATH=./112-latentcoref:./112-latentcoref/countTable:${lbjlib}:${modeldir}:${classpath}:${dataDir}:${resourceDir}:${stanfordsys}
echo $CLASSPATH
nice java -Xmx$1g -cp ${CLASSPATH} edu.illinois.cs.cogcomp.lbj.coref.main.MainClass $2 $3 $4 $5 $6 $7 $8 $9 
