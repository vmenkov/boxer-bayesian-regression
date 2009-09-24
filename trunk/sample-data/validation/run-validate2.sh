#!/bin/csh

# This script runs BOXER on a small sample

source set.sh


set t=train:${d}/sample.xml
set v=validate:${d}/sample.xml

#    read-suite:${d}/situation1-fallback-suite.xml \


echo "The following run should fail, because the suite is inconsistent"
time java $opt -Dmodel=tg $driver read-suite:${d}/suite2.xml \
   $v  write-suite:suite-out.xml





