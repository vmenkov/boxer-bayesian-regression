#!/bin/csh

#-- Ontology matching

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

time java $opt -Dverbosity=1 -Dlearner=notg-learner-param-eta=0_001.xml $onto \
 read:$wits/safeWITS_2006_03.1-100.txt:out-giant-model-1e-8.xml  \
 test:$wits/safeWITS_2006_03.1-100.txt \
 test:$wits/safeWITS_2006_03.101-200.txt \
  > adaptive-wits-both.log


# time java $opt -Dverbosity=1  -Dlearn.rep=1 -Dlearner=notg-learner-param-eta=0_001.xml -Dsd=true -Dadaptive=true $onto $wits/safeWITS_2006_03.1-100.txt $wits/safeWITS_2006_03.1-100.txt > adaptive-wits-self.log

#time java $opt -Dverbosity=1  -Dlearn.rep=1 -Dlearner=notg-learner-param-eta=0_001.xml -Dsd=true -Dadaptive=true $onto $wits/safeWITS_2006_03.1-100.txt $wits/safeWITS_2006_03.101-200.txt > adaptive-wits-block2.log


#time java $opt -Dverbosity=1 -Dlearner=notg-learner-param-eta=0_001.xml $onto block1.csv block1.csv > self.tmp
#tail -18 self.tmp| perl -p tmp.pl > confusion-self.tmp 
#time java $opt -Dverbosity=1 -Dlearner=notg-learner-param-eta=0_001.xml $onto block1.csv block2.csv > block2.tmp
#tail -18 block2.tmp| perl -p tmp.pl > confusion-block2.tmp 




