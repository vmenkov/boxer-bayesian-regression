#!/bin/csh

#-- This is a sample script for Ontology matching, illustrating
#-- use of symmetric methods directly based on columns' frequency
#-- vectors (command vec_js - Jensen-Shannon Divergence)
#-- 

set cp=`../scripts/findBoxerJar.pl`

#-- Setting JVM options (memory used, classpath)
set opt="-Xmx256m"
if ("$cp" != "") then
  set opt="${opt} -cp ${cp}"
else
  echo "Location of boxer.jar is not known."  
endif

echo "Using the following JVM options: ${opt}"

set rep=edu.dimacs.mms.applications.learning.Repeater

time java $opt -Dverbosity=0 -Dsd=true -Dadaptive=true -Drandom=0 -Dr=99 -DM=99 -Deps=0.001 $rep read-suite:conso-train-suite.xml read-learner:notg-learner-param-eta=0_001.xml train:conso-train.xml   test:conso-test.xml > conso-asd.log


