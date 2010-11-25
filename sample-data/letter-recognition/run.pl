#!/usr/bin/perl -s

use strict;

# This script runs BOXER on the letter-recognition set
# Usage: run.sh out-dir learner-param.xml


if ($#ARGV < 0) {
    print "Please specify config fle and, optionally output directory, i.e.:
$0 param.xml out-dir-name\n";
    exit 1;
}

my $pkg ="letter-recognition";

my $random = ($0 =~ /\brandom.pl$/) ? 1 : 0;

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
    $out = ($random?   "$main/out/$pkg/random/${base}" :
    "$main/out/$pkg/${base}" );
}


print "Will try to create output directory $out\n";


if (-e $out) {
    die "Directory $out already exists";
}

mkdir( $out);

if (!(-d $out)) {
    die "Failed to create directory $out; please make sure that the path is correct";
}


my $driver="edu.dimacs.mms.applications.learning.Driver";
my $cp="${main}/classes:${main}/lib/xercesImpl.jar";
my $opt="-Xmx256m -cp ${cp}";

my $d="${main}/letter-recognition";

`cp $learner $out`;

#-- -Dmodel=tg
#-- -Dmodel=eg

my $nr = 1;

if ($random) {
    $nr=500;
    $opt .= " -Drandom=$nr";
}

`java $opt -Dout=${out} -DM=100 -Dverbosity=0 $driver read-suite:${d}/letter-recognition-suite.xml read-learner: $learner train:${d}/letter-recognition.xml : ${out}/myscores.txt > ${out}/run.log`;

`select.pl $out`;

my $title=$base;
$title =~ s/-/ /g; 
$title =~ s/_/\./g;
if ($random) {
    $title .= " (avg of $nr runs)";
}

if ($pkg eq "letter-recognition") {
    $title = "26 Letters. $title";
}

print "Plot title should be: $title\n";
`perl -pe "s/Title/$title/" cmd.gnu > ${out}/cmd.gnu`;


