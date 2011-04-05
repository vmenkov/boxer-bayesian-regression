#!/bin/csh

#-- Runs BOXER Repeater on the Gazetteer data set

echo 0=$0

#-- Setting JVM options (memory used, classpath)
set conv=edu.dimacs.mms.applications.util.CsvToXml;
set rep=edu.dimacs.mms.applications.learning.Repeater
set cp=$HOME/boxer/lib/boxer.jar
set opt="-Xmx256m -cp ${cp}" 

foreach w (true false)
foreach g (0 1 2 3) 

if ($w == "true") then
  set wx=1
else
  set wx=0
endif

set d=w${wx}-g${g}
echo Directory $d
mkdir $d

#-- Tokenizer run
set copt="${opt} -Dinput.words=${w} -Dinput.gram=${g}"
echo copt=$opt
java  $copt -DdicOut=tmp-train.dic $conv train.csv schema.txt $d/train-suite.xml $d/train.xml
java  $copt -DdicIn=tmp-train.dic $conv test.csv schema.txt $d/test-suite.xml $d/test.xml

#-- Classifier run
echo "Using the following JVM options: ${opt}"
/usr/bin/time java $opt -Dverbosity=1 -Dsd=true -Dadaptive=true -Drandom=0 -Deps=0.00001  -Dr=428 -DM=428 $rep read-suite:$d/train-suite.xml read-learner:sgd-learner-param-eta=0.01.xml train:$d/train.xml:$d/train-scores.out  test:$d/test.xml:$d/test-scores.out >& $d/gazetteer.log

end
end



