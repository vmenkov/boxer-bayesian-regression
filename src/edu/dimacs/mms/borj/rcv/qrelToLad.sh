#!/bin/csh


set rcv=${home}/rcv
set main=${home}/boxer
set out=${main}/rcv-out

set cp=${main}/classes:${main}/lib/xercesImpl.jar

set d=${home}/rcv
set opt="-cp $cp -Dd=${d} -Dout=${out}"


time java $opt -Dqrel=true -Dfrom=1 -Dto=1000 -Dd=${home}/rcv-lad -Dout=out-lad-1.dat \
edu.dimacs.mms.borj.rcv.RcvToLAD \
${home}/rcv-lad/regions.sampled.ids \
${rcv}/lyrl2004_tokens_train.dat 

time java $opt -Dqrel=true -Dfrom=1001 -Dto=2000 -Dd=${home}/rcv-lad -Dout=out-lad-2.dat \
edu.dimacs.mms.borj.rcv.RcvToLAD \
${home}/rcv-lad/regions.sampled.ids \
${rcv}/lyrl2004_tokens_train.dat 


#--  this is for the name file
time java $opt -Dqrel=true -Dfrom=1 -Dto=2000 -Dd=${home}/rcv-lad -Dout=out-lad-both.dat \
edu.dimacs.mms.borj.rcv.RcvToLAD \
${home}/rcv-lad/regions.sampled.ids \
${rcv}/lyrl2004_tokens_train.dat 

#time java $opt -Dfrom=544179 -Dto=545178 edu.dimacs.mms.borj.rcv.RcvToLAD ${out}/rcv-boxer-ids-random.txt 

