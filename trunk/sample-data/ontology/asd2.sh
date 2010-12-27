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

#-- Create and apply a model

#time java $opt -Dverbosity=1  -Dlearn.rep=1 -Dlearner=notg-learner-param-eta=0_001.xml -Dlearn.sd=true -Dlearn.adaptive=true $onto \
#train:$wits/safeWITS_2006_03.1-100.txt \
#write:out-giant-model-empty-1e-3.xml \
#test:$wits/safeWITS_2006_03.1-100.txt \
#> adaptive-wits-self-both-empty-1e-3-gen.log


#-- train on small DS, test on big DS

# java $opt -Dverbosity=1  -Dlearn.rep=1 -Dlearner=notg-learner-param-eta=0_001.xml -Dlearn.sd=true -Dlearn.adaptive=true $onto \
#train:export_08Dec10.csv \
#test:$wits/safeWITS_2006_03.1-100.txt \
#> export-vs-safeWITS.log

#-- Apply precomputed model

time java $opt -Dverbosity=1  $onto \
read:$wits/safeWITS_2006_03.1-100.txt: out-giant-model-empty-1e-8.xml \
test:$wits/Sent.2010.12.21.WITS_2006_03.1-100.txt \
test:$wits/Sent2010.12.21WITS_2006_04.1-100.txt \
test:$wits/Sent2010.12.21WITS_2006_04.500-600.txt \
> safe2006_03.1-100-vs-sent2006-03-and-04.log


#time java $opt -Dverbosity=1 -Dlearner=notg-learner-param-eta=0_001.xml $onto \
# read:$wits/safeWITS_2006_03.1-100.txt: out-giant-model-empty-1e-8.xml \
# test:export_08Dec10.csv \
#  > safeWITS-vs-export.log

#time java $opt -Dverbosity=1 -Dlearner=notg-learner-param-eta=0_001.xml $onto \
# read:$wits/safeWITS_2006_03.1-100.txt: out-giant-model-empty-1e-8.xml \
# test:$wits/safeWITS_2006_03.1-100.txt \
# test:$wits/safeWITS_2006_03.101-200.txt \
#  > adaptive-wits-both-empty-1e-8.log


#time java $opt -Dverbosity=1  -Dlearn.rep=1 -Dlearner=notg-learner-param-eta=0_001.xml -Dlearn.sd=true -Dlearn.adaptive=true $onto $wits/safeWITS_2006_03.1-100.txt $wits/safeWITS_2006_03.101-200.txt > adaptive-wits-block2.log


#time java $opt -Dverbosity=1 -Dlearner=notg-learner-param-eta=0_001.xml $onto block1.csv block1.csv > self.tmp
#tail -18 self.tmp| perl -p tmp.pl > confusion-self.tmp 
#time java $opt -Dverbosity=1 -Dlearner=notg-learner-param-eta=0_001.xml $onto block1.csv block2.csv > block2.tmp
#tail -18 block2.tmp| perl -p tmp.pl > confusion-block2.tmp 




