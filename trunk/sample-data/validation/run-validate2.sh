#!/bin/csh

#-- This script runs BOXER on a small sample. It demonstrates  an
#-- example of using the "validate" command

source ../set.sh

echo "Using the following JVM options: ${opt}"
echo "Reading datasets from $d, suites from $s"

set t=train:${d}/sample.xml
set v=validate:${d}/sample.xml

echo "The following run should fail, because the suite is inconsistent"
time java $opt -Dmodel=tg $driver read-suite:${s}/suite2.xml \
   $v  write-suite:suite-out.xml

