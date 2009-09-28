#!/bin/csh


set main=${home}/boxer
set out=${main}/rcv-out

set cp=${main}/classes:${main}/lib/xercesImpl.jar

set d=${home}/rcv
set opt="-cp $cp -Dd=${d} -Dout=${out}"


time java $opt -Dfrom=1 -Dto=10000  edu.dimacs.mms.borj.rcv.QrelToXML ${out}/rcv-boxer-ids-random.txt 

time java $opt -Dfrom=544179 -Dto=545178 edu.dimacs.mms.borj.rcv.QrelToXML ${out}/rcv-boxer-ids-random.txt 

