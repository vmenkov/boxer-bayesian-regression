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

set borj=edu.dimacs.mms.borj.Driver

java $opt -Dmodel=tg $borj train:conso-train.xml test:conso-train.xml test:conso-test.xml write:model-out.xml


