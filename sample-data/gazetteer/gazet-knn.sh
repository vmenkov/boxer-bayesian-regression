#!/bin/csh

#-- Runs BOXER Repeater on the Gazetteer data set

echo 0=$0

#-- Setting JVM options (memory used, classpath)
set conv=edu.dimacs.mms.applications.util.CsvToXml;
set rep=edu.dimacs.mms.applications.learning.Repeater
set cp=$HOME/boxer/lib/boxer.jar
set opt="-Xmx256m -cp ${cp}" 

set k=3

#foreach w (true false)
#foreach g (0 1 2 3 4) 
foreach w (true false)
foreach g (5) 

if ($w == "true") then
  set wx=1
else
  set wx=0
endif

set db=../gazetteer-out
set d=$db/knn-k${k}-w${wx}-g${g}
echo Directory $d
mkdir $d

#-- Tokenizer run
set copt="${opt} -Dinput.words=${w} -Dinput.gram=${g}"
echo copt=$opt
set dic=${db}/tmp-train.dic
java  $copt -DdicOut=${dic} $conv $db/train.csv schema.txt $d/train-suite.xml $d/train.xml
java  $copt -DdicIn=${dic} $conv $db/test.csv schema.txt $d/test-suite.xml $d/test.xml

#-- Classifier run
echo "Using the following JVM options: ${opt}"
/usr/bin/time java $opt -Dverbosity=1 -Drandom=0 -Dr=428 -DM=428 $rep read-suite:$d/train-suite.xml read-learner:knn-learner-param-k=${k}.xml train:$d/train.xml:$d/train-scores.out  test:$d/test.xml:$d/test-scores.out >& $d/gazetteer.log

end
end



