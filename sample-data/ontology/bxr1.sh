#!/bin/csh

#-- Ontology matching, using BXR

set cp=`../scripts/findBoxerJar.pl`

#-- Setting JVM options (memory used, classpath)
set opt="-Xmx256m"
if ("$cp" != "") then
  set opt="${opt} -cp ${cp}"
else
  echo "Location of boxer.jar is not known."  
endif

echo "Using the following JVM options: ${opt}"

set onto=edu.dimacs.mms.applications.ontology.Driver;

set wits=$HOME/boxer/WITS

#-- Create and apply a model

set log=bxr.log

time java $opt -Dverbosity=1 -Dlearn.bxr=true -Dlearn.eps=0.01 $onto \
 train:$wits/safeWITS_2006_03.1-100.txt \
 test:$wits/safeWITS_2006_03.1-100.txt \
 test:$wits/Sent2010.12.21WITS_2006_04.500-600.txt  >& $log


set dir=matrix-bxr-1e-2
./matrix2html.pl $log
mkdir $dir
mv matrix-?.* $log  out-safeWITS_2006_03.1-100.xml $dir

