#!/bin/csh


foreach x (1 2 3)

set out=out-$x

echo Output dir is $out
mkdir $out

java -cp ../../../lib/boxer.jar edu.dimacs.mms.borj.Driver \
read-suite:  SimpleTestSuite.xml \
read-priors: cheating-with-priors-${x}.xml \
read-learner:  tg-learner-param-eta=1_0-g=1_0-K=1.xml  \
train: SimpleTestData-part-1.xml \
test: SimpleTestData-part-1.xml : $out/scores-part-1-out.dat \
test: SimpleTestData-part-2.xml : $out/scores-part-2-out.dat \
write-priors:$out/out-priors.xml \
write:$out/out-learners.xml >& $out/out.txt

end
