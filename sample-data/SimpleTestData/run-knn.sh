#!/bin/csh

#-- Runs BOXER kNN classifier

echo 0=$0

#-- Setting JVM options (memory used, classpath)
set rep=edu.dimacs.mms.applications.learning.Repeater
set cp=$HOME/boxer/lib/boxer.jar
set opt="-Xmx256m -cp ${cp}" 


set d=$HOME/boxer/out/SimpleTestData/knn

echo Directory $d
mkdir $d
if (!(-d $d)) then
    echo "Failed to create directory $d; please make sure that the path is correct"
    exit
endif

foreach k (1 3 5 7) 


#-- Classifier run
/usr/bin/time java $opt -Dverbosity=1 -Drandom=0 -Dr=264 -DM=264 $rep read-suite:SimpleTestSuite.xml \
read-learner:../learners/knn-learner-param-k=${k}.xml \
train:SimpleTestData-part-1.xml:$d/train-scores.out  \
test:SimpleTestData-part-2.xml:$d/test-scores.out   >& $d/knn-k=${k}.log

end
