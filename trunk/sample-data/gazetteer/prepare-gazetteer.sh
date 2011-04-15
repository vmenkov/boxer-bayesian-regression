#!/bin/csh


#-- prepares training and test sets
echo 'Label,Name,Text' >  head.csv

head -1 KDD_Name_Set.csv | perl -pe 's/^/Label,Name,/' > head-people.csv
tail -481 KDD_Name_Set.csv | perl -pe 's/^(.*?)\s*\r?\n?$/person,$1,$1\n/' > tmp.csv
./split.pl tmp.csv 0.25 tmp-people-a.csv tmp-people-b.csv

perl -pe 's/^(.*?)\s*\r?\n?$/place,"$1","$1"\n/' Locations.txt.loc  > tmp.csv
./split.pl  tmp.csv 0.10 tmp-places-a.csv tmp-places-b.csv

cat head.csv tmp-people-a.csv  tmp-places-a.csv > train.csv
cat head.csv tmp-people-b.csv  tmp-places-b.csv > test.csv

#-- Setting JVM options (memory used, classpath)
set conv=edu.dimacs.mms.applications.util.CsvToXml;
set cp=$HOME/boxer/lib/boxer.jar
set opt="-Xmx256m" 
set opt="${opt} -cp ${cp} -Dinput.gram=4"


echo opt=$opt
java  $opt -DdicOut=tmp-train.dic $conv train.csv schema.txt train-suite.xml train.xml
java  $opt -DdicIn=tmp-train.dic $conv test.csv schema.txt test-suite.xml test.xml
