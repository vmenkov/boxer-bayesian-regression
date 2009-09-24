#!/bin/csh

# This script runs BOXER on a small sample

source ./set.sh

#set d=${main}/src/sample-data/validation
set d=.

set t=train:${d}/sample.xml
set v=validate:${d}/sample.xml

#    read-suite:${d}/situation1-fallback-suite.xml \

echo "The following run, with the command ${v}, should succeed"
time java $opt -Dmodel=tg $driver $v  write-suite:suite-out.xml


echo The following run should succeed as well
time java $opt -Dmodel=tg $driver read-suite:${d}/suite1.xml \
   $v  write-suite:${out}/suite-out.xml





