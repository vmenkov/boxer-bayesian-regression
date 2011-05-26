#!/bin/csh

#-- Runs BOXER Repeater on the SimpleTestData set

#-- Setting JVM options (memory used, classpath)
set conv=edu.dimacs.mms.applications.util.CsvToXml;
set rep=edu.dimacs.mms.applications.learning.Repeater
set cp=$HOME/boxer/lib/boxer.jar
set opt="-Xmx256m -cp ${cp}" 


set d=$HOME/boxer/out/SimpleTestData/asd

echo Directory $d
mkdir $d
if (!(-d $d)) then
    echo "Failed to create directory $d; please make sure that the path is correct"
    exit
endif

set eps=1e-8

#-- Classifier run
echo "Using the following JVM options: ${opt}"
/usr/bin/time java $opt -Dverbosity=1 -Dsd=true -Dadaptive=true -Drandom=0 -Deps=${eps}  -Dr=264 -DM=264 $rep read-suite:SimpleTestSuite.xml read-learner:sgd-learner-param-eta=0.01.xml \
train:SimpleTestData-part-1.xml:$d/train-scores.out  \
test:SimpleTestData-part-2.xml:$d/test-scores.out   >& $d/asd-eps=${eps}.log

