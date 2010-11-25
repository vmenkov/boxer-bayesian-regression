#!/bin/csh

# This script runs BOXER on a small sample
# Usage: run.sh out-dir learner-param.xml


set learner=$1
if ($learner == "") then
  set learner=./notg-learner-param.xml
endif

set base=`basename $learner .xml`

set base=`echo $base | perl -pe 's/-learner-param//'`

set out=$2
if ($out == "") then
    set out=/home/vmenkov/boxer/out/SimpleTestData/${base}
endif

echo "Will try to create output directory $out"


if ($out == "") then
    echo "Please specify config fle and, optionally output directory, i.e.:"
    echo "$0 param.xml out-dir-name"
    exit 1
else if (-e $out) then
    echo "Directory $out already exists"
   exit 1
endif

mkdir $out

if (!(-d $out)) then
    echo "Failed to create directory $out; please make sure that the path is correct"
    exit
endif


# source ./set.sh
set driver=edu.dimacs.mms.applications.learning.Driver
set main=${home}/boxer
set cp=${main}/classes:${main}/lib/xercesImpl.jar
set opt="-Xmx256m -cp ${cp}"

set d=${main}/boxer-bayesian-regression/sample-data/SimpleTestData

cp $learner $out

#-- -Dmodel=tg
#-- -Dmodel=eg


#time java $opt -Dout=${out} -DM=1 -Dverbosity=0 $driver \
#    read-suite: SimpleTestSuite.xml    read-learner: $learner \
#   train: SimpleTestData.xml : ${out}/myscores.txt > ${out}/run.log

select.pl $out

set title=`echo $base | perl -pe 's/-/ /g; s/_/\./g'`
perl -pe "s/Title/$title/" cmd.gnu > ${out}/cmd.gnu

