#!/bin/csh

source ./set.sh

# This example tests reading the model created by run-rcv-small-aa.sh
# (which involved training the model 3 times on the first 1000
# vectors), and then applying it to the same data as in that script
# and in run-rcv-small-ab.sh

set qa=read-labels:${d}/rcv-boxer-ids-random-1-10000-qrel.xml
set qb=read-labels:${d}/rcv-boxer-ids-random-544179-545178-qrel.xml
set a=train:${d}/train1000.xml 
set a1=test:${d}/train1000.xml
set w1=write:${out}/rcv-model3-copy.xml
set r=read:${out}/rcv-model3-out.xml
set b=test:${d}/last1000.xml

#-- read the model and apply it to the training set 
time java $opt $driver \
    $r $qa $a1 $w1 > $out/test-read-rcv-model3-a.log 

#-- read the model and apply it to the test set
time java $opt $driver \
    $r $qb $b  > $out/test-read-rcv-model3-b.log 
