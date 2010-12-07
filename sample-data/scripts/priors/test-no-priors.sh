#!/bin/csh


source ../set.sh

set o=$out/priors-0
mkdir $o

echo "Using the following JVM options: ${opt}"
echo "Reading datasets from $dm, suites from $sm, output to $o"

java $opt $driver \
read-suite:$sm/sample-suite.xml \
read-learner:$lm/tg-learner-param-K=1.xml \
train:$dm/train-set.xml \
test:$dm/train-set.xml:$o/sample-train-scores-out.dat  \
test:$dm/test-set.xml:$o/sample-test-scores-out.dat  \
write:$o/out-learners.xml > $o/out.txt

