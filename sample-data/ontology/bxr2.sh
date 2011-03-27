#!/bin/csh

#-- Ontology matching, using BXR.

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

#--  apply a pre-created model

set log=bxr.log

time java $opt -Dverbosity=1 -Dlearn.bxr=true -Dlearn.eps=0.1 $onto \
 read:$wits/safeWITS_2006_03.1-100.txt:/tmp/safeWITS_2006_03.1-100_1301085307429.model \
 test:$wits/safeWITS_2006_03.1-100.txt  > $log

set dir=matrix-bxr-1e-1
./matrix2html.pl $log
mkdir $dir
mv matrix-?.* $log  out-safeWITS_2006_03.1-100.xml $dir



