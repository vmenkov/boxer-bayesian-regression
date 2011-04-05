#!/usr/bin/perl

use strict;

if ($#ARGV != 3) {
    die("Usage: $0 input.csv train-percentage out-train.csv out-test.csv\n");
}

my ($in, $pct, $out1, $out2) = @ARGV;

if ($pct < 0 || $pct > 1) {
    die "Training set percentage must be between 0.0 and 1.0\n";
}

open(F, "<$in") or die "Cannot read $in\n";

open(G1, ">$out1") or die "Cannot write $in\n";
open(G2, ">$out2") or die "Cannot write $in\n";

my $s=undef;
my $cnt1=0;
my $cnt2=0;

while(defined ($s=<F>)) {
    if ($s=~ /^\s*\#/ || $s =~ /^\s*$/) { next; }
    my $r1No = $cnt1 /($cnt1 + $cnt2 + 1);
    my $r1Yes = ($cnt1+1) /($cnt1 + $cnt2 + 1);
    if ($pct <= ($r1Yes + $r1No)/2) {
	#-- no
	print G2 $s;
	$cnt2++;
    } else {
	#-- yes
	print G1 $s;
	$cnt1++;
    }
}


close(F);
close(G1);
close(G2);
