#!/bin/csh

# This script runs BOXER on a small sample

source set.sh

set d=${main}/src/sample-data

set a=train:${d}/train-set.xml
set a1=test:${d}/train-set.xml 
set b=test:${d}/test-set.xml

time java  $opt $driver \
    read-learner: ${d}/tg-learner-param.xml \
    $a $a1 $b \
    write-suite: ${out}/sample-suite-out.xml write: ${out}/sample-blob-tg-1.xml



