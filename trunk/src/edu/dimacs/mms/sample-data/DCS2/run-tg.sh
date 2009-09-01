#!/bin/csh

# This script runs BOXER on a small sample

source set.sh

set d=${main}/src/sample-data/DCS2

set ta=train:${d}/sample.xml 
set tb=train:${d}/sample2.xml 

#    read-learner:$d/tg-learner-param.xml \

#-- First run
echo FIRST RUN
time java $opt -Dmodel=tg $driver \
    read-suite:${d}/suite1.xml    $ta \
    write-suite:${out}/DCS2-suite-out1.xml \
    write:${out}/DCS2-model-out1.xml 

#-- restore and continue
echo SECOND RUN
time java $opt $driver \
    read:${out}/DCS2-model-out.xml $tb \
    write-suite:${out}/DCS2-suite-out2.xml \
    write:${out}/DCS2-model-out2.xml


