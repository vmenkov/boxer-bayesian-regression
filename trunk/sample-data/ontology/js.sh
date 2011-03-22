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

#-- word-based features only:
# -Dinput.gram=0 -Dinput.empty.skip=false -Dinput.empty.special=true

#-- single-char-based features only:
# -Dinput.words=false -Dinput.gram=1 -Dinput.empty.skip=false -Dinput.empty.special=true \

#-- single-char-based features and word-based features:
# -Dinput.words=true -Dinput.gram=1 -Dinput.empty.skip=false -Dinput.empty.special=true \

#set logbase=${sym}-word-gram1-prev-safe-vs-self
#set log=${logbase}.log
#set dir=matrix-${logbase}
#time java $opt -Dverbosity=1  -Dvec.mode=PREVALENCE \
#-Dinput.words=true -Dinput.gram=1 -Dinput.empty.skip=false -Dinput.empty.special=true \
#$onto ${sym}:$wits/safeWITS_2006_03.1-100.txt:$wits/safeWITS_2006_03.1-100.txt > $log

#cp $log $log.bak
#./matrix2html.pl -div $log
#mkdir $dir
#mv matrix-?.* $log  $dir

set logbase=${sym}-word-gram1-prev-safe-vs-sent2
set log=${logbase}.log
set dir=matrix-${logbase}
time java $opt -Dverbosity=1  -Dvec.mode=PREVALENCE \
-Dinput.words=true -Dinput.gram=1 -Dinput.empty.skip=false -Dinput.empty.special=true \
$onto ${sym}:$wits/safeWITS_2006_03.1-100.txt:$wits/Sent2010.12.21WITS_2006_04.500-600.txt  > $log

cp $log $log.bak
./matrix2html.pl -div $log
mkdir $dir
mv matrix-?.* $log  $dir

set logbase=${sym}-word-gram2-prev-safe-vs-sent2
set log=${logbase}.log
set dir=matrix-${logbase}
time java $opt -Dverbosity=1  -Dvec.mode=PREVALENCE \
-Dinput.words=true -Dinput.gram=2 -Dinput.empty.skip=false -Dinput.empty.special=true \
$onto ${sym}:$wits/safeWITS_2006_03.1-100.txt:$wits/Sent2010.12.21WITS_2006_04.500-600.txt  > $log

cp $log $log.bak
./matrix2html.pl -div $log
mkdir $dir
mv matrix-?.* $log  $dir
