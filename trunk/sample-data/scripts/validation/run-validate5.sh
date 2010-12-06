#!/bin/csh

#-- This script runs BOXER on a small sample. It demonstrates a parser
#-- failure on an invalid data sets

source ../set.sh

echo "Using the following JVM options: ${opt}"
echo "Reading datasets from $d, suites from $s"

set v=validate:${d}/validSample.xml
set train=train:${d}/validSample.xml
set test=test:${d}/validSample.xml


echo "The following run should succeed"
time java $opt -Dverbosity=2 -Dmodel=tg $driver $v $train $test






