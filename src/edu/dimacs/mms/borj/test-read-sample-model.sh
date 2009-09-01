#!/bin/csh

# This script runs BOXER on a small sample

source set.sh


set d=${main}/src/sample-data

set a=train:${d}/train-set.xml 
set a1=test:${d}/train-set.xml 
set b=test:${d}/test-set.xml

time java $opt $driver \
    read:${out}/sample-model.xml \
    write:${out}/sample-model-copy.xml \
    $a1    $b


