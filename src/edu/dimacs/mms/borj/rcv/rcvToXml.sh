#!/bin/csh


set main=${home}/boxer
set cp=${main}/classes:${main}/lib/xercesImpl.jar
set out=${main}/rcv-out
set d=${home}/rcv
set opt="-cp $cp -Dd=${home}/rcv"

set args="edu.dimacs.mms.borj.rcv.RcvToXML ${out}/rcv-boxer-ids-random.txt ${d}/lyrl2004_tokens_train.dat ${d}/lyrl2004_tokens_test_pt?.dat"
# Use -Dqrel=true to also include QREL data to XML

time java $opt -Dout=${out}/train100.xml -Dfrom=1 -Dto=100 $args
time java $opt -Dout=${out}/train+q-100.xml -Dfrom=1 -Dto=100 -Dqrel=true $args


time java $opt -Dout=${out}/train1000.xml -Dfrom=1 -Dto=1000 $args
time java $opt -Dout=${out}/train+q-1000.xml -Dfrom=1 -Dto=1000 -Dqrel=true $args

time java $opt -Dout=${out}/train10000.xml -Dfrom=1 -Dto=10000 $args
time java $opt -Dout=${out}/train+q-10000.xml -Dfrom=1 -Dto=10000 -Dqrel=true $args

time java $opt -Dout=${out}/last1000.xml -Dfrom=544179 -Dto=545178  $args
time java $opt -Dout=${out}/last+q-1000.xml -Dfrom=544179 -Dto=545178  -Dqrel=true $args


