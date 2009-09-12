#!/bin/csh

# This script runs BOXER on a small sample

source set.sh

#set d=${main}/src/sample-data/validation

set t=train:${d}/sample.xml
set v=validate:${d}/sample.xml

#    read-suite:${d}/situation1-fallback-suite.xml \


#-- this should fail, because the suite is inconsistent
time java $opt -Dverbosity=2 -Dmodel=tg $driver read-suite:${d}/suite3.xml \
   $v  write-suite:suite-out.xml





