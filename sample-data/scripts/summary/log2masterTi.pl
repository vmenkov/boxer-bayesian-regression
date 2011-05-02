#!/usr/bin/perl -s

require 'logutil.pm';

#-- An auxiliary script for producing the summary table for Paul
#-- (2011-04-26).  This script reads the "original" master file, looks
#-- for @RunLog directives, and replaces them with the data extracted
#-- from the log files mentioned there.

#-- This script is for the Topic Identification task log files


#[TRAIN SCORES][train.xml: -1 : 428+000]Label^person: Recall=101/120=0.842, Precision=101/115=0.878
#[TRAIN SCORES][train.xml: -1 : 428+000]Label^place: Recall=294/308=0.955, Precision=294/313=0.939
#[TRAIN LOG LIN][train.xml: -1 : 428+000][Label] -0.1915766682558447 0.8825873127928617
#[TRAIN WARECALL][train.xml: -1 : 428+000][Label] 0.9228971962616822

use strict;

sub usage() {
    print "Usage:\n";
    print "  $0 [-base=\$HOME/boxer/boxer-bayesian-regression/sample-data/] -masterplan=master.txt\n";
    print "or, to process a single log file:\n";
    print "  $0 -log=run.log\n";
}

if (defined $::masterplan) {
    &processMaster( $::masterplan);
} elsif (defined $::log) {
    &processLog( $::log);
} else {
    &usage();
}

#-- copies the master file to stdout, replacing each 
#-- @RunLog=foo.log with the data extracted from $::base/foo.log
sub processMaster($) {
    my ($master) = @_;
    open (M, $master) or die "Cannot read $master\n";
    my $s=undef;
      
    my $r = 'RunLog';
    my $rdtag = '@' . $r;

    while(defined ($s=<M>)) {

	if ($s =~ /^\s*$rdtag=(\S*)/) {
	    my $val=$1;
	    print "$r=$val\n";
	    my $log = $val;
	    if (defined $::base && $log !~ /^\//) {
		$log = "$::base/$log";
	    }
	    &processLog($log);
	} else {
	    print $s;
	}
    }
    close(M);
}


#-- Gets data from one log file
sub processLog($) {

    my ($q) = @_;
    my($logFile,$logDir) = &validateLogFileName( $q );

    my @allTrain=();
    my @allTest=();

    my $s=undef;
    
    while(defined ($s=<F>)) {
	if ($s=~ /^\[TRAIN SCORES\]/) {
	    push @allTrain, $s;
	} elsif ($s=~ /^\[TEST SCORES\]/) {
	    push @allTest, $s;
	} elsif ($s =~ /BOXER toolkit \(version ([\d\.]+)\)/ ) {
	    my $ver = $1;
	    print "Version=$ver\n";
	} elsif ($s =~ /\[TIME\]\[START\] (\S\S\S \d+, \d+)\s+\((\d+)\)/) {
	    my ($datestring, $timestamp)=($1,$2);
	    print "Date=$datestring\n";
	} elsif ($s =~ /([\d\.]+)user\s+([\d\.]+)system/) {
	    # Found the timer line, e.g. 
	    # 31.44user 515.94system
	    my ($u,$s) = ($1,$2);
	    my $t=$u+$s;
	    print "Run_Time=$t\n";
	}
    }
      
    my $trainSize = &findSetSize(\@allTrain);

    my @classDescriptions = ();
    my %classDesc2trainInfo = ();
    my %classDesc2testInfo = ();


#    print STDERR "train:  " . @allTrain . "; size=$trainSize\n"; 
    foreach my $s (@allTrain) {
	my ($c, $t) = &doLine($s, "TrainingSet", $trainSize);
	push @classDescriptions, $c;
	$classDesc2trainInfo{$c} = $t;
	$classDesc2testInfo{$c} = '';
    }

    my $testSize = &findSetSize(\@allTest);
    foreach my $s (@allTest) {
	my ($c, $t) = 	&doLine($s, "TestSet", $testSize);
	#-- push @classDescriptions, $c;
	$classDesc2testInfo{$c} = $t;
    }
    close(F);

    foreach my $c (@classDescriptions) {
	print '@class{'."\n";
	print $c;
	print $classDesc2trainInfo{$c};
	print $classDesc2testInfo{$c};
	print '}'."\n";
    } 
}	      

#-- finds the set size (by summing up class sizes from "Recall" reports)
sub findSetSize($) {
    my ($pset)=@_;
    my @set= @$pset;
    my $ourDis=undef;
    my $size=0;
    foreach my $s (@set) {
	if ($s =~ /\](\S+?)\^(\S+?): Recall=(\d+)\/(\d+).*?, Precision=(\d+)\/(\d+)/) {	
	    my ($disLabel, $classLabel) = ($1,$2);
	    my ($truePos,$dataPos,$dummy, $systemPos) = ($3,$4,$5,$6);
	    if (!defined $ourDis) {
		$ourDis=$disLabel;
	    } elsif ($ourDis ne $disLabel) {
		last;
	    }
	    $size += $dataPos;
	}
    }
    return $size;
}

sub doLine($$$) {
    my ($s,$setLabel,$n) = @_;
    if ($s =~ /\](\S+?)\^(\S+?): Recall=(\d+)\/(\d+).*?, Precision=(\d+)\/(\d+)/) {
	my ($disLabel, $classLabel) = ($1,$2);
	my ($truePos,$dataPos,$dummy, $systemPos) = ($3,$4,$5,$6);
	($dummy == $truePos) or die "TP mismatch: $dummy != $truePos\n";
	my $dataN = $n - $dataPos;
	my $falsePos = $systemPos - $truePos;
	my $detection = ($dataPos==0) ? 0 : $truePos/$dataPos;
	my $falseAlarm =  ($dataN==0) ? 0 :$falsePos/$dataN;
	my @pairs1 = 
	    (
	     "DiscriminationName=$disLabel",
	     "ClassName=$classLabel");
	my @pairs2 =
	    (
	     "${setLabel}Positive_N=$dataPos",
	     "${setLabel}Complement_N=$dataN",
	     "${setLabel}TruePos_N=$truePos",
	     "${setLabel}FalsePos_N=$falsePos",
	     "${setLabel}Detection=$detection",
	     "${setLabel}FalseAlarm=$falseAlarm");
	return ( join("\n", @pairs1) . "\n",
		 join("\n", @pairs2) . "\n");
    }
}
