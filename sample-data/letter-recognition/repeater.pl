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

my $pkg ="letter-recognition";

my $base=$learner;
$base =~ s/\.xml$//;
$base =~ s/-learner-param//;

my $home = "/home/vmenkov";
my $main= "${home}/boxer";


my $out;

if ($#ARGV >=1) {
    $out = $ARGV[1];
} else {
    $out = "$main/out/$pkg/repeater/";
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
my $d="${main}/$pkg";

`cp $learner $out`;

#-- -Dmodel=tg
#-- -Dmodel=eg

`java $opt -Dout=${out} -DM=500 -Dr=200000 -Drandom=$nr -Dverbosity=0 $driver read-suite:${d}/letter-recognition-suite.xml  read-learner:$learner train:${d}/letter-recognition-part-1.xml  test:${d}/letter-recognition-part-2.xml > ${out}/run.log`;

`select.pl $out`;

my $title=$base;
$title =~ s/-/ /g; 
$title =~ s/_/\./g;
$title .=    " (random repeat - $nr time". ($nr>1? "s":"") .")";

if ($pkg eq "letter-recognition") {
    $title = "26 Letters. $title";
}


print "Plot title should be: $title\n";
`perl -pe "s/Title/$title/" cmd.gnu > ${out}/cmd.gnu`;


