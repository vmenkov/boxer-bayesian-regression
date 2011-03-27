#!/bin/csh

#-- This script converts the Consolidated_PDF_Table_Data.csv file, and its
#-- training and test sections into XML files (for BOXER applications) and
#-- BMR files (for BXR). When creating BMR files, care is taken to
#-- use the same feature dictionary throughout, so that the numeric feature
#-- identifiers used in multiple BMR files are consistent.

set cp=`../scripts/findBoxerJar.pl`

#-- Setting JVM options (memory used, classpath)
set opt="-Xmx256m"
if ("$cp" != "") then
  set opt="${opt} -cp ${cp}"
else
  echo "Location of boxer.jar is not known."  
endif

echo "Using the following JVM options: ${opt}"

set conv=edu.dimacs.mms.applications.util.CsvToXml

#edu.dimacs.mms.applications.util.CsvToXml
#edu/dimacs/mms/applications/util/CsvToXml.class

head -1 Consolidated_PDF_Table_Data.csv > conso-head.csv

#-- 190 lines in total, including the header. Split 1+99+90
head -100 Consolidated_PDF_Table_Data.csv > conso-train.csv
tail -90 Consolidated_PDF_Table_Data.csv > tmp.csv
cat conso-head.csv tmp.csv > conso-test.csv

set parseOpt="-Dinput.gram=0"

set in=Consolidated_PDF_Table_Data.csv
set suite=conso-suite.xml
set dataset=conso.xml

java -cp $cp $opt $parseOpt -Dinput.gram=0 $conv $in schema.in $suite $dataset
java -cp $cp $opt $parseOpt -Dinput.gram=0 -DdicOut=conso-dic.xml $conv $in schema.in $suite conso.bmr

set in=conso-train.csv
set suite=conso-train-suite.xml
set dataset=conso-train.xml

java -cp $cp $opt $parseOpt -Dinput.gram=0 $conv $in schema.in $suite $dataset
java -cp $cp $opt $parseOpt -Dinput.gram=0 -DdicIn=conso-dic.xml  $conv $in schema.in $suite conso-train.bmr

set in=conso-test.csv
set suite=conso-test-suite.xml
set dataset=conso-test.xml

java -cp $cp $opt $parseOpt -Dinput.gram=0 $conv $in schema.in $suite $dataset
java -cp $cp $opt $parseOpt -Dinput.gram=0 -DdicIn=conso-dic.xml  $conv $in schema.in $suite conso-test.bmr

