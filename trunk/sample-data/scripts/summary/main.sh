#!/bin/csh

#-- The main script for generating CSV results summary files.
#-- The human-editable files are kdd-ti-master-scils.txt and kdd-onto-master-scils.txt;
#-- based on them, and on the log files referred to from them, CSV files are built

./log2masterTi.pl -base=$HOME/boxer/boxer-bayesian-regression/sample-data -masterplan=kdd-topicid-master-scils.txt > ti.tmp
./master2csv.pl ti.tmp > master-topicid.csv

./log2masterOnto.pl -base=$HOME/boxer/boxer-bayesian-regression/sample-data/ontology -masterplan=kdd-onto-master-scils.txt > onto.tmp
./master2csv.pl -onto onto.tmp > master-onto.csv

