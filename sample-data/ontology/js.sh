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

set onto=edu.dimacs.mms.applications.ontology.Driver;

set wits=$HOME/boxer/WITS

set sym=vec_js


#set log=${sym}-safe-vs-self.log
#set dir=matrix-${sym}-safe-vs-self
#time java $opt -Dverbosity=1  -Dvec.mode=TF \
#-Dinput.gram=0 -Dinput.empty.skip=false -Dinput.empty.special=true \
#$onto ${sym}:$wits/safeWITS_2006_03.1-100.txt:$wits/safeWITS_2006_03.1-100.txt > $log


set log=${sym}-safe-vs-sent.log
set dir=matrix-${sym}-safe-vs-sent
time java $opt -Dverbosity=1  -Dvec.mode=TF \
-Dinput.gram=0 -Dinput.empty.skip=false -Dinput.empty.special=true \
$onto ${sym}:$wits/safeWITS_2006_03.1-100.txt:$wits/Sent2010.12.21WITS_2006_04.1-100.txt  > $log

cp $log $log.bak

./matrix2html.pl -div $log
mkdir $dir
mv matrix-?.* $log  $dir

