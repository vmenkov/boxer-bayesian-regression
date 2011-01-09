#!/bin/csh

#-- This is a sample script for Bayesian Ontology matching, illustrating
#-- use of "symmetric" matching methods (commands sym1 and sym2)
#-- 
#-- See also asd2.sh, which contains more commands.

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

#set sym=sym1
set sym=sym2

#set log=${sym}-self.log
#set dir=matrix-${sym}-self

#time java $opt -Dverbosity=1  -Dlearn.rep=1 -Dlearner=notg-learner-param-eta=0_001.xml -Dlearn.sd=true -Dlearn.adaptive=true -Dlearn.eps=1e-3 \
#-Dinput.gram=0 -Dinput.empty.skip=false -Dinput.empty.special=true \
#$onto ${sym}:$wits/safeWITS_2006_03.1-100.txt:$wits/safeWITS_2006_03.1-100.txt > $log

set log=${sym}-safe-vs-sent.log
set dir=matrix-${sym}-safe-vs-sent

time java $opt -Dverbosity=1  -Dlearn.rep=1 -Dlearner=notg-learner-param-eta=0_001.xml -Dlearn.sd=true -Dlearn.adaptive=true -Dlearn.eps=1e-8 \
-Dinput.gram=0 -Dinput.empty.skip=false -Dinput.empty.special=true \
$onto ${sym}:$wits/safeWITS_2006_03.1-100.txt:$wits/Sent2010.12.21WITS_2006_04.1-100.txt  > $log

cp $log $log.bak

./matrix2html.pl $log
mkdir $dir
mv matrix-?.* $log  $dir

