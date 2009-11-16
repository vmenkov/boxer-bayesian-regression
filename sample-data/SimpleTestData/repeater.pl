#!/usr/bin/perl

use strict;

# This script runs BOXER on a small sample
# Usage: run.sh out-dir learner-param.xml


if ($#ARGV < 0) {
    print "Please specify config fle and, optionally output directory, i.e.:
$0 param.xml out-dir-name\n";
    exit 1;
}

my $random = ($0 =~ /\brandom-repeater.pl$/) ? 1 : 0;

my $nr = ($random? 500 : 1);

my $learner= ($#ARGV >=0) ? $ARGV[0] : "./notg-learner-param.xml";


my $base=$learner;
$base =~ s/\.xml$//;
$base =~ s/-learner-param//;

my $home = "/home/vmenkov";
my $main= "${home}/boxer";


my $out;

if ($#ARGV >=1) {
    $out = $ARGV[1];
} else {
    $out = "$main/out/SimpleTestData/repeater/";
    if ($nr > 1) { $out .= "${nr}/"; }
    $out .= $base;
}


print "Will try to create output directory $out\n";


if (-e $out) {
    die "Directory $out already exists";
}

mkdir( $out);

if (!(-d $out)) {
    die "Failed to create directory $out; please make sure that the path is correct";
}


my $driver="edu.dimacs.mms.accutest.Repeater";
my $cp="${main}/classes:${main}/lib/xercesImpl.jar";
my $opt="-Xmx256m -cp ${cp}";

# my $d="${main}/boxer-bayesian-regression/sample-data/SimpleTestData";

`cp $learner $out`;

#-- -Dmodel=tg
#-- -Dmodel=eg

`java $opt -Dout=${out} -DM=10 -Dr=5000 -Drandom=$nr -Dverbosity=0 $driver     read-suite: SimpleTestSuite.xml    read-learner: $learner    train: SimpleTestData-part-1.xml  test: SimpleTestData-part-2.xml > ${out}/run.log`;

`select.pl $out`;

my $title=$base;
$title =~ s/-/ /g; 
$title =~ s/_/\./g;
$title .=    " (random repeat - $nr time". ($nr>1? "s":"") .")";


print "Plot title should be: $title\n";
`perl -pe "s/Title/$title/" cmd.gnu > ${out}/cmd.gnu`;


