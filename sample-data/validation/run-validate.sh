#!/bin/csh

# This script runs BOXER on a small sample

source ./set.sh

#set d=${main}/src/sample-data/validation
echo d=$d

set t=train:${d}/sample.xml
set v=validate:${d}/sample.xml

#    read-suite:${d}/situation1-fallback-suite.xml \

#-- this should succeed
time java $opt -Dmodel=tg $driver $v  write-suite:suite-out.xml


#-- this should succeed too
time java $opt -Dmodel=tg $driver read-suite:${d}/suite1.xml \
   $v  write-suite:suite-out.xml





