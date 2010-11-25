#!/usr/bin/perl

use strict;

# This script runs BOXER on a small sample
# Usage: run.sh out-dir learner-param.xml


if ($#ARGV < 0) {
    print "Please specify config fle and, optionally output directory, i.e.:
$0 param.xml out-dir-name\n";
    exit 1;
}

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
    $out = "$main/out/orthogonal/repeater/${base}";
}


print "Will try to create output directory $out\n";


if (-e $out) {
    die "Directory $out already exists";
}

mkdir( $out);

if (!(-d $out)) {
    die "Failed to create directory $out; please make sure that the path is correct";
}


my $driver="edu.dimacs.mms.applications.learning.Repeater";
my $cp="${main}/classes:${main}/lib/xercesImpl.jar:${main}/lib/xml-apis.jar";

my $opt="-Xmx256m -cp ${cp}";

# my $d="${main}/boxer-bayesian-regression/sample-data/orthogonal";

`cp $learner $out`;

#-- -Dmodel=tg
#-- -Dmodel=eg

my $nr = 1;


`java $opt -Dout=${out} -DM=1 -Dr=5000 -Dverbosity=0 $driver     read-suite: SimpleTestSuite.xml    read-learner: $learner    train: orthogonal.xml  test: orthogonal.xml > ${out}/run.log`;

`./select.pl $out`;

my $title=$base;
$title =~ s/-/ /g; 
$title =~ s/_/\./g;
$title .= " (random repeat)";

print "Plot title should be: $title\n";
`perl -pe "s/Title/$title/" cmd.gnu > ${out}/cmd.gnu`;


