#!/bin/csh

# This script prepares the list of "eligible" documents

set main=${home}/boxer
set out=${main}/rcv-out
set cp=${main}/classes:${main}/lib/xercesImpl.jar


if (-e $out) then
   echo Reusing directory $out
else
   echo Creating directory $out
   mkdir $out
endif

time java -cp $cp -Dd=${home}/rcv -Dout=$out edu.dimacs.mms.borj.rcv.PrepareRCV  > $out/prepare.log
