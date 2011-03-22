#!/bin/csh

#-- This is a sample script for Ontology matching, illustrating
#-- use of symmetric methods directly based on columns' frequency
#-- vectors (commands vec_cos and vec_js)
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

set sym=vec_cos


set logbase=${sym}-word-gram2-safe-vs-self
set log=${logbase}.log
set dir=matrix-${logbase}


time java $opt -Dverbosity=1  -Dvec.mode=TF \
-Dinput.word=true -Dinput.gram=2 -Dinput.empty.skip=false -Dinput.empty.special=true \
$onto ${sym}:$wits/safeWITS_2006_03.1-100.txt:$wits/safeWITS_2006_03.1-100.txt  > $log


cp $log $log.bak
./matrix2html.pl $log
mkdir $dir
mv matrix-?.* $log  $dir


set logbase=${sym}-word-gram2-safe-vs-sent1
set log=${logbase}.log
set dir=matrix-${logbase}

time java $opt -Dverbosity=1  -Dvec.mode=TF \
-Dinput.word=true -Dinput.gram=2 -Dinput.empty.skip=false -Dinput.empty.special=true \
$onto ${sym}:$wits/safeWITS_2006_03.1-100.txt:$wits/Sent2010.12.21WITS_2006_04.1-100.txt  > $log

cp $log $log.bak
./matrix2html.pl $log
mkdir $dir
mv matrix-?.* $log  $dir

set logbase=${sym}-word-gram2-safe-vs-sent2
set log=${logbase}.log
set dir=matrix-${logbase}

time java $opt -Dverbosity=1  -Dvec.mode=TF \
-Dinput.word=true -Dinput.gram=2 -Dinput.empty.skip=false -Dinput.empty.special=true \
$onto ${sym}:$wits/safeWITS_2006_03.1-100.txt:$wits/Sent2010.12.21WITS_2006_04.500-600.txt  > $log

cp $log $log.bak
./matrix2html.pl $log
mkdir $dir
mv matrix-?.* $log  $dir

