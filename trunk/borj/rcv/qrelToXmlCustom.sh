#!/bin/csh


set main=${home}/boxer
set in=${main}/rcv-out
set out=${main}/rcv-out/qrel-custom

set cp=${main}/classes:${main}/lib/xercesImpl.jar

set d=${home}/rcv
set opt="-cp $cp -Dd=${d} -Dout=${out}"


time java $opt -Dfrom=1 -Dto=10000  borj.rcv.QrelToXML ${in}/rcv-boxer-ids-random.txt  ${in}/rcv-even-smaller-suite.xml

#time java $opt -Dfrom=544179 -Dto=545178 borj.rcv.QrelToXML ${out}/rcv-boxer-ids-random.txt  rcv-even-smaller-suite.xml

