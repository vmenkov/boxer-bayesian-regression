#!/usr/bin/perl -s

use strict;


# This script runs the BOXER "Repeater" application
# (edu.dimacs.mms.applications.learning.Repeater), which is a small BOXER
# application that offers examples from a given training set, in a
# random order, to a BOXER learner.

#-- Usage: run.sh  [-random=0|1]  learner-param.xml out-dir


if ($#ARGV < 0) {
    print "Please specify config fle and, optionally output directory, i.e.:
$0 param.xml out-dir-name\n";
    print "Example: $0 [-random=0|1] notg-learner-param.xml /tmp/new-out-dir\n";
    exit 1;
}

my $nr = 
    ($::random? $::random : 0);

my $learner= ($#ARGV >=0) ? $ARGV[0] : "./notg-learner-param.xml";
(-e $learner) or die "No learner file $learner exists!\n";

my $base=$learner;
$base =~ s/(.*\/)//;
$base =~ s/\.xml$//;
$base =~ s/-learner-param//;

my $home = $ENV{'HOME'};
my $main= "${home}/boxer";


my $out;

if ($#ARGV >=1) {
    $out = $ARGV[1];
} else {
    $out = "$main/out/orthogonal/repeater";
    if ($nr > 1) { $out .= "/${nr}"; }
    $out .= "/${base}";
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
my $cp="${main}/lib/boxer.jar";

my $opt="-Xmx256m -cp ${cp}";

`cp $learner $out`;

#-- -Dmodel=tg
#-- -Dmodel=eg

`java $opt -Dout=${out} -DM=1 -Dr=5000 -Drandom=$nr -Dverbosity=0 $driver     read-suite: SimpleTestSuite.xml    read-learner: $learner    train: orthogonal.xml  test: orthogonal.xml > ${out}/run.log`;

`../scripts/select.pl $out`;

my $title=$base;
$title =~ s/-/ /g; 
$title =~ s/_/\./g;
$title .=  
    ($nr==0) ? " (non-random)" :
    " (random repeat - $nr time". ($nr>1? "s":"") .")";

print "Plot title should be: $title\n";
`perl -pe "s/Title/$title/" cmd.gnu > ${out}/cmd.gnu`;


