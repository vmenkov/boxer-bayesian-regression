#!/bin/csh

#-- This shell script is used to build the documentation that comes
#-- with BOXER and BORJ. Most of the process is done by a Javadoc
#-- command, which generates HTML doc files from Java files and copies
#-- appropriately positioned (i.e., placed in the appropriate
#-- package_name/doc-files directory) HTML files to the same
#-- destination directory.

#-- This is preceded by copying a few additional XML
#-- files to the doc directory.

#-- Usage:
#  javadoc.sh  [destination-dir]

set ver=`grep version boxer/Version.java | perl -pe 's/[^0-9\.]//g'`
echo "As per Version.java, the current version is ${ver}"

set cp=../classes:../lib/xercesImpl.jar 

#-- The destination directory
set d=$1


#-- default value for the destination directory
if ("$d" == "") then 
    set d=../javadoc
endif

if (-e $d) then
   echo Reusing destination directory $d
else
   echo Creating destination directory $d
   mkdir $d
endif

#-- Copy some XML files that you want to become part of the documentation
#set sampleFrom=sample-data
#set sampleTo=boxer/doc-files
foreach x (sample.xml sample-suite-out.xml eg-learner-param-1.xml ) 
   cp sample-data/${x} boxer/doc-files
end

javadoc -protected -d $d -sourcepath . -classpath $cp \
 -use -link http://java.sun.com/j2se/1.5.0/docs/api/ -header "<em>BOXER ${ver}</em>" -windowtitle "BOXER API" -overview overview.html  boxer borj.rcv borj tokenizer

# -sourcepath . -subpackages
