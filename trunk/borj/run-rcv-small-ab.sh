#!/bin/csh

source ./set.sh

#echo java $opt $driver read-suite:${d}/rcv-suite.xml train:${d}/vec.xml 


# In this example, we train the model on the first 1000 examples, and
# test it on the last 1000, and then repeat his 3 times

set qa=read-labels:${d}/rcv-boxer-ids-random-1-10000-qrel.xml
set qb=read-labels:${d}/rcv-boxer-ids-random-544179-545178-qrel.xml
set a=train:${d}/train1000.xml 
set b=test:${d}/last1000.xml:${out}/rcv-small-ab-scores-tg.txt 
set w=write:${out}/model-out.xml


time java $opt $driver \
    read-suite:${d}/rcv-small-cat-suite.xml  $qa $qb \
    $a $b    $a $b    $a $b $w > $out/run-rcv-small-ab.log 
