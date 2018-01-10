#!/bin/bash

mkdir -p logs

bin/illinois-pos-server.sh -p 9091 >& logs/pos.log &

bin/illinois-chunker-server.sh -p 9092 >& logs/chunk.log &

bin/illinois-coref-server.sh -p 9094 >& logs/coref.log &

bin/stanford-parser-server.sh -p 9095 >& logs/stanford.log &

bin/illinois-verb-srl-server.sh -p 14810 >& logs/verb-srl.log &

bin/illinois-nom-srl-server.sh -p 14910 >& logs/nom-srl.log &

bin/illinois-wikifier-server.sh -p 15231 >& logs/wikifier.log &

bin/illinois-ner-extended-server.pl ner-ext 9096 configs/ner.conll.config >& logs/ner-ext-conll.log &

bin/illinois-ner-extended-server.pl ner-ext 9097 configs/ner.ontonotes.config >& logs/ner-ext-ontonotes.log &

bin/berkeley-parser-server.sh -p 16000 >& logs/berkeley.log &




#The non-java components need to be started too:

cd CharniakServer
./start_charniak.sh 9987 charniak9987 >& ../logs/charniak.log &

cd -


# start the curator. It may take a few minutes before the larger
#    annotators are ready to receive requests; until then, curator's log
#    will show errors for these servers. 

bin/curator.sh --annotators configs/annotators-example.xml --port 9010 --threads 10 >& logs/curator.log &
