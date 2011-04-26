#!/bin/csh

#--

set cp=$HOME/boxer/lib/boxer.jar
set data=$HOME/boxer/bio/split

#-- Setting JVM options (memory used, classpath)
set opt="-Xmx256m"
if ("$cp" != "") then
  set opt="${opt} -cp ${cp}"
else
  echo "Location of boxer.jar is not known."  
endif

echo "Using the following JVM options: ${opt}"

set rep=edu.dimacs.mms.applications.learning.Repeater

set train=$HOME/boxer/bio/split/train/train.xml
set test=$HOME/boxer/bio/split/test/test.xml
set cnt=`grep -c datapoint.name $train`
set param=sgd-learner-param-eta=1.xml

/usr/bin/time java $opt -Dverbosity=0 -Dsd=true -Dadaptive=true -Drandom=0 -Dr=$cnt -DM=$cnt -Deps=0.01 $rep read-suite:bio2-suite.xml read-learner:$param train:$train   test:$test > bio-asd-eps=1e-2.log


