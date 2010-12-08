#!/usr/bin/perl -s

use strict;

# This script runs the BOXER "Repeater" application
# (edu.dimacs.mms.applications.learning.Repeater), which is a small BOXER
# application that offers examples from a given training set, in a
# random order, to a BOXER learner.

# Usage: run.sh [-random=nn | -random=0] [-priors=priors.xml] learner-param.xml out-dir
#
# Sample:
# ./repeater.pl -random=500 -priors=priors-low-var-4.xml tg-learner-param-eta=1_0-g=1_0.xml test-out

if ($#ARGV < 0) {
    print "Please specify config file and, optionally output directory, i.e.:
$0 [-random=500] [-priors=priors.xml]  param.xml out-dir-name\n";
    print "Use -random=1 for a single randomized run, and -random=0 for a 
single non-randomized run\n";
    exit 1;
}

#my $random = ($0 =~ /\brandom-repeater.pl$/) ? 1 : 0;

my $nr = 
    ($::random? $::random : 0);

my $learner= ($#ARGV >=0) ? $ARGV[0] : "./notg-learner-param.xml";
(-e $learner) or die "No learner file $learner exists!\n";


my $base=$learner;
$base =~ s/\.xml$//;
$base =~ s/-learner-param//;

#-- These defaults are only appropriate if you indeed have ~/boxer
my $home = $ENV{'HOME'};
my $main= "${home}/boxer";

my $out;

if ($#ARGV >=1) {
    $out = $ARGV[1];
} else {
    $out = "$main/out/SimpleTestData/repeater";
    if ($nr > 1) { $out .= "/${nr}"; }
    $out .= "/${base}";
}

my $priors = "";
if (defined $::priors) {
    if (!-f $::priors) {
	die "There is no file named '$::priors'\n";
    }
    $priors = "read-priors:" . $::priors;
    print "Will use the extra command: $priors\n";
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


`java $opt -Dout=${out} -DM=10 -Dr=5000 -Drandom=$nr -Dverbosity=0 $driver read-suite: SimpleTestSuite.xml $priors  read-learner: $learner    train: SimpleTestData-part-1.xml  test: SimpleTestData-part-2.xml > ${out}/run.log`;

`../scripts/select.pl $out`;

my $title=$base;
$title =~ s/-/ /g; 
$title =~ s/_/\./g;
$title .=  
    ($nr==0) ? " (non-random)" :
    " (random repeat - $nr time". ($nr>1? "s":"") .")";


print "Plot title should be: $title\n";
`perl -pe "s/Title/$title/" cmd.gnu > ${out}/cmd.gnu`;


