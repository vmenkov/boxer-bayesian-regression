#!/bin/csh

#-- This is a sample script for Bayesian Ontology matching, illustrating
#-- use of various options. See also asd2.sh, which contains more commands
#-- of the same kind.

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

set log=emptySkip.log

java $opt -Dverbosity=1  -Dlearn.rep=1 -Dlearner=notg-learner-param-eta=0_001.xml -Dlearn.sd=true -Dlearn.adaptive=true -Dlearn.eps=1e-3 \
-Dinput.gram=0 -Dinput.empty.skip=false  -Dinput.empty.special=true \
$onto train:$wits/safeWITS_2006_03.1-100.txt test:$wits/safeWITS_2006_03.1-100.txt > $log

./matrix2html.pl $log
 mv matrix-?.* $log  out-safeWITS_2006_03.1-100.xml matrix-1e-3-empty=special/


#java $opt -Dverbosity=1  -Dlearn.rep=1 -Dlearner=notg-learner-param-eta=0_001.xml -Dlearn.sd=true -Dlearn.adaptive=true -Dlearn.eps=0.001 \
#-Dinput.empty.skip=false  -Dinput.empty.special=true \
#$onto \
#train:$wits/safeWITS_2006_03.1-100.txt test:$wits/safeWITS_2006_03.1-100.txt \
#> emptySpecial.log
