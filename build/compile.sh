#!/bin/csh

# This script compiles all Java files

set d=../classes
set cp=${d}:../lib/xercesImpl.jar:../lib/jena.jar

if (-e $d) then
   echo Reusing directory $d
else
   echo Creating directory $d
   mkdir $d
endif

set opt=-Xlint:unchecked

#  -Xlint:deprecation
#javac $opt -cp $cp -d $d boxer/*.java borj/*.java borj/rcv/*.java tokenizer/*.java

javac $opt -cp $cp -d $d src/edu/dimacs/mms/borj/*.java src/edu/dimacs/mms/boxer/*.java src/edu/dimacs/mms/tokenizer/*.java




