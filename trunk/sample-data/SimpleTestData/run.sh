#!/bin/csh

# This script runs BOXER on a small sample

# source ./set.sh
set driver=edu.dimacs.mms.accutest.Driver
set main=${home}/boxer
set cp=${main}/classes:${main}/lib/xercesImpl.jar
set opt="-Xmx256m -cp ${cp}"

set d=${main}/boxer-bayesian-regression/sample-data/SimpleTestData

#    read-learner:$d/eg-learner-param.xml \

time java $opt -DM=20 -Dverbosity=0 -Dmodel=eg $driver \
    read-suite: SimpleTestSuite.xml \
    train: SimpleTestData.xml : myscores.txt > run.log



