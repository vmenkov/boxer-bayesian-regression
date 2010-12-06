#!/bin/csh

#-- This script runs BOXER on a small sample. It demonstrates a parser
#-- failure on an invalid data sets

source ../set.sh

echo "Using the following JVM options: ${opt}"
echo "Reading datasets from $d, suites from $s"

set v=validate:${d}/invalidSample.xml


echo "The followin run should fail, because the data set is invalid"
time java $opt -Dverbosity=2 -Dmodel=tg $driver $v






