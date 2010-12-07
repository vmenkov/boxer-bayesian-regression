#!/bin/csh


set out=out-1

echo Output dir is $out
mkdir $out

java -cp ../../../lib/boxer.jar edu.dimacs.mms.borj.Driver \
read-suite: ../sample-suite.xml \
read-priors: ../sample-priors-2.xml \
read-learner: ../tg-learner-param-K=1.xml \
train: ../train-set.xml \
test: ../train-set.xml:$out/sample-train-scores-out.dat  \
test: ../test-set.xml:$out/sample-test-scores-out.dat  \
write-priors:$out/out-priors.xml \
write:$out/out-learners.xml > $out/out.txt



