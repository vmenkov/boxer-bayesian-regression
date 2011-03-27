#!/bin/csh

#-- Ontology matching, using ASD

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

set log=asd.log

(/usr/bin/time java $opt -Dverbosity=1  -Dlearn.rep=1 -Dlearner=notg-learner-param-eta=0_001.xml -Dlearn.sd=true -Dlearn.adaptive=true -Dlearn.eps=1e-8 $onto \
 train:$wits/safeWITS_2006_03.1-100.txt \
 test:$wits/safeWITS_2006_03.1-100.txt \
 test:$wits/Sent2010.12.21WITS_2006_04.500-600.txt  >& $log)>& time.txt

set dir=matrix-asd-1e-8
./matrix2html.pl $log
mkdir $dir
mv matrix-?.* $log time.txt $dir




