#!/bin/csh

source ./set.sh

#echo java $opt $driver read-suite:${d}/rcv-suite.xml train:${d}/vec.xml 

# In this example, the test set is the same as the training set. 
# We repeat training 3 times

set qa=read-labels:${d}/rcv-boxer-ids-random-1-10000-qrel.xml
#set qb=read-labels:${d}/rcv-boxer-ids-random-544179-545178-qrel.xml
set a=train:${d}/train1000.xml 
set a1=test:${d}/train1000.xml
set w=write:${out}/rcv-model-trivial-out.xml


time java -Dmodel=trivial $opt $driver \
    read-suite:${d}/rcv-small-cat-suite.xml  $qa \
    $a $a1    $a $a1    $a $a1 $w > $out/run-rcv-small-aa-trivial.log 
