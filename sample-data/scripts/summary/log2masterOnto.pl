#!/usr/bin/perl -s


require 'logutil.pm';

#-- An auxiliary script for producing the summary table for Paul
#-- (2011-04-26).  This script reads the "original" master file, looks
#-- for @RunLog directives, and replaces them with the data extracted
#-- from the log files mentioned there.
#--
#-- This script is for the Ontology task log files

use strict;

sub usage() {
    print "Usage:\n";
    print "  $0 [-base=\$HOME/boxer/boxer-bayesian-regression/sample-data/] -masterplan=master-onto.txt\n";
    print "E.g.:\n";
    print "  $0 -base=\$HOME/boxer/boxer-bayesian-regression/sample-data/ontology -masterplan=kdd-onto-master-scils.txt\n";
    print "Or, to process a single log file:\n";
    print "  $0 -log=run.log\n";
}

#-- row counts for data sources (full path to int)
my %dsRows = ();

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
 	     
    #-- guess learner name and params from the dire name
    my $logDirMain = $logDir;
    $logDirMain =~ s|.*/||;

    #my $learner = undef;
    #my $params = params;
    if ($logDirMain =~ /-(asd)-/ ||
	$logDirMain =~ /-(sd)-/ ||
	$logDirMain =~ /-(sgd)-/ ||
	$logDirMain =~ /-(sym1)-/ ||
	$logDirMain =~ /-(knn)-/ ||
	$logDirMain =~ /-(bxr)-/ ) {
	my ($learner, $params) = ($1, $');  # extract learner name and param ');
	$learner =~ tr/a-z/A-Z/;
	print "Learner=$learner\n";
	print "Parameters=$params\n";
    }
    my $s=undef;
    my $timeIsSet=0;
    
    my @matinfo=();

    my $ds2;
    my $ds2_cols;
    my $ds2_rows;
    my $combiner;

    while(defined ($s=<F>)) {
	if ($s =~ /BOXER toolkit \(version ([\d\.]+)\)/i ) {
	    my $ver = $1;
	    print "Version=$ver\n";
	} elsif ($s =~ /^(\S\S\S \d+, \d+) \d\d:\d\d:\d\d \SM edu.dimacs.mms.boxer.Logging info/ && !$timeIsSet) {
	    my $datestring=$1;
	    print "Date=$datestring\n"; 
	    $timeIsSet=1;
	} elsif ($s=~ /^Input options: (.*)/) {
	    print "Representation=$1\n";
	} elsif ($s =~ /^Reading (DS[12]) from file: (.*)/) {
	    my ($ds,$path) = ($1,$2);
	    if (!defined $dsRows{$path}) {
		$dsRows{$path} = &findRowCnt($path);
	    }
	    my $rows = $dsRows{$path};
	    if ($ds eq 'DS1') {
		print  "$ds=$path\n";
		if (defined $rows) { 
		    print  "${ds}_Rows=$rows\n";
		}
	    } else {
		#-- remember for future use
		$ds2 = $path;
		$ds2_rows = $rows;
	    }
	} elsif ($s =~ /([\d\.]+)user\s+([\d\.]+)system/) {
	    # Found the timer line, e.g. 
	    # 31.44user 515.94system
	    my ($u,$s) = ($1,$2);
	    my $t=$u+$s;
	    print "Run_Time=$t\n";
	} elsif ($s =~ /Confusion matrix\s*-\s+(\S.*\S),/) {
	    $combiner = $1;
	} elsif ($s =~ /INFO: 0 <=i< (\d+); 0 <=j< (\d+)/) {
	    print "DS1_Columns=$2\n";
	    $ds2_cols=$1;
	    push @matinfo, [$ds2, $ds2_cols, $ds2_rows, $combiner];
	}

    }     
    close(F);

    my $cnt=0;
    foreach my $info (@matinfo) {
	my ($ds2, $ds2_cols, $ds2_rows, $combiner) = @$info;
	$cnt++;
	print '@class{'."\n";
	print "DS2=$ds2\n";
	if (defined $ds2_rows) { 
	    print  "DS2_Rows=$ds2_rows\n";
	}
	print "DS2_Columns=$ds2_cols\n";
	print "Combiner=$combiner\n";
	my $matrixFile = "$logDir/matrix-$cnt.html";
	&parseMatrixFile($matrixFile);
	print '}'."\n";
    } 
}	      

#-- HTML scraper on matrix-*.html, which was produced by matrix2html.pl
sub parseMatrixFile($) {
    my ($matrixFile) = @_;

    if (-e $matrixFile) {
	open(MAT, $matrixFile) or die "Cannot read log file $matrixFile\n";
	print STDERR "Reading $matrixFile\n";
    } elsif (-e "${matrixFile}.gz") {
	open(MAT, "zcat ${matrixFile}.gz |") or die "Cannot run zcat ${matrixFile}.gz\n";
	print STDERR "zcat ${matrixFile}.gz\n";
    } else {
	die "No file named $matrixFile or ${matrixFile}.gz can be found\n";
    }

    my @rows = grep {/<li>/} <MAT>;
    close(MAT);

#<li>33 are the top ones in their row,
#<li>42 are in the top 2,
#<li>45 are in the top 3

    my @topcnt=();
    foreach my $s (@rows) {
	if ($s =~ /<li>(\d+) are the top/) {
	    $topcnt[0] = $1;
	} elsif  ($s =~ /<li>(\d+) are in the top ([23])/) {
	    $topcnt[$2-1] = $1;
	}
    }

    foreach my $i (0..$#topcnt) {
	if (defined($topcnt[$i])) {
	    print "Top". ($i+1) . "=" . $topcnt[$i] . "\n";
	}
    }
   

}

#-- row count for a data source
sub findRowCnt($) {
    my ($f) = @_;
    (-f $f && -r $f) or return undef;
    my @res = `wc $f`;
    # print STDERR join(":", @res);
    return ($res[0] =~ /(\d+)/) ? $1-1: undef;
}
