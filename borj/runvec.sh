#!/bin/csh

# This scripts expects a single QREL+data file

source set.sh


set a=train:${d}/train+q-1000.xml 
set a1=test:${d}/train+q-1000.xml 
set b=test:${d}/last+q-1000.xml
set w=write:${out}/model-big-out.xml

# 
time java $opt $driver read-suite:${d}/rcv-suite.xml \
    $a $a1    $a $a1    $a $a1 $w > $out/train+q-1000-aa.log

time java $opt  $driver read-suite:${d}/rcv-suite.xml \
    $a $b    $a $b    $a $b $w > $out/train+q-1000-ab.log

