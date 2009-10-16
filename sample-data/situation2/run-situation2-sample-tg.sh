#!/bin/csh

# This script runs BOXER on a small sample

source set.sh

set d=${main}/boxer-bayesian-regression/sample-data/situation2

set a=train:${d}/situation2-train-a.xml 
set b=train:${d}/situation2-train-b.xml 
set atest=test:${d}/situation2-train-a.xml:${out}/sample-situation2-scores-a-tg.txt 
set btest=test:${d}/situation2-train-b.xml:${out}/sample-situation2-scores-b-tg.txt 
set c=test:${d}/situation2-test.xml:${out}/sample-situation2-scores-c-tg.txt 

#    read-learner:$d/tg-learner-param.xml \

time java $opt -Dmodel=tg $driver \
    read-suite:${d}/situation2-fallback-suite.xml \
    $a $b $atest    $btest $c \
    write-suite:${out}/situation2-suite.xml write:${out}/situation2-model.xml


