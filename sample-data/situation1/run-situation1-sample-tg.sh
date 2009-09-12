#!/bin/csh

# This script runs BOXER on a small sample

source set.sh

set d=${main}/src/sample-data/situation1

set a=train:${d}/situation1-train-a.xml 
set b=train:${d}/situation1-train-b.xml 
set atest=test:${d}/situation1-train-a.xml:${out}/sample-situation1-scores-a-tg.txt 
set btest=test:${d}/situation1-train-b.xml:${out}/sample-situation1-scores-b-tg.txt 
set c=test:${d}/situation1-test.xml:${out}/sample-situation1-scores-c-tg.txt 

#    read-learner:$d/tg-learner-param.xml \

time java $opt -Dmodel=tg $driver \
    read-suite:${d}/situation1-fallback-suite.xml \
    $a $b $atest    $btest $c \
    write-suite:${out}/situation1-suite.xml write:${out}/situation1-model.xml


