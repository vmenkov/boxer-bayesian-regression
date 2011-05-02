#!/usr/bin/perl -s

#-- An auxiliary script for converting the summary table to CSV or
#-- HTML (as requested by Paul, 2011-04-26)

# Usage example
# ./log2master.pl  -base=$HOME/boxer/boxer-bayesian-regression/sample-data/ -masterplan=kdd-topicid-master.txt > master.tmp
# ./master2csv.pl  master.tmp > summary-ti.csv

# ./log2masterOnto.pl  -base=$HOME/boxer/boxer-bayesian-regression/sample-data/ -masterplan=kdd-onto-master.txt > master.tmp
# ./master2csv.pl -onto master.tmp > summary-onto.csv

#--- or pipe them:
# ./log2masterOnto.pl -base=$HOME/boxer/boxer-bayesian-regression/sample-data/ontology -masterplan=kdd-onto-master-scils.txt |  ./master2csv.pl -onto 


#-- master table syntax:
#  @series {
#  ....
#  @run{
#  name=val
#   ...
#  @class{
#  name=val
#   ....
#  }
#  }
#  }

use strict;

#-- Ontology mode? (Vs. topic identification mode)
my $onto= ($::onto ? 1:0);

#--------------------------------------
#-- read the list of field names
my $fieldsFile = $onto? "fields-onto.txt" : "fields-ti.txt";
open(F,$fieldsFile) or die "Cannot read $fieldsFile\n";

my @legalNames = ();
my $s=undef;
while( defined ($s=<F>)) {
    $s = &trim($s);
    #-- skip comments and empty lines
    if ($s eq '') { next; }
    push @legalNames, $s;
}

#print STDERR join(";", @legalNames )."\n";

close(F);

#--------------------------------
#-- process the master data file (stding, or cmd line args)

my $seriesTag='@series{';
my $runTag='@run{';
my $classTag='@class{';
my $endTag='}';

&printHeader();

&finishSeries( &createHash());

#--------------------------------------------------------
#  @series { @series { ....  @run{...} @run{...} ....} }
sub finishSeries() {
    my ($pa)=@_;
    my %baseH=%$pa;
    my $s=undef;

    while( defined ($s=<>)) {
	$s= &trim($s);
	if ($s eq '' ) { next; }
	elsif ($s=~ /$seriesTag/) {
	    &finishSeries(&copyHash(\%baseH));
	} elsif ($s=~ /$runTag/) {
	    &finishRun(&copyHash(\%baseH));
	} elsif ($s =~ /^(\S+?)=(.*)/) {
	    &insert(\%baseH, $1,$2);
	}  elsif ( $s eq $endTag) {
	    return;
	} else {
	    die "Cannot parse the following line as a command (in series context) or a name=val pair: $s\n";
	}
    }
}

#--------------------------------------------------------
#  @run { ....  @class{...} @class{...} ....} }
sub finishRun() {
    my ($pa)=@_;
    my %baseH=%$pa;
    my $s=undef;

    while( defined ($s=<>)) {
	$s= &trim($s);
	if ($s eq '' ) { next; }
	elsif ($s=~ /$runTag/ || $s=~ /$seriesTag/) {
	    die "Cannot nest a series or a run inside a run!\n";
	} elsif ($s=~ /$classTag/) {
	    &finishClass(&copyHash(\%baseH));
	}  elsif ( $s eq $endTag) {
	    return;
	} elsif ($s =~ /^(\S+?)=(.*)/) {
	    &insert(\%baseH, $1,$2);
	} else {
	    die "Cannot parse the following line as a name=val pair: $s\n";
	}
    }
    print STDERR "File ended before \@Run closed\n";
}

sub finishClass($) {
    my ($pa)=@_;
    my %h=%{$pa};
    while( defined ($s=<>)) {
	$s= &trim($s);
	if ($s eq '' ) { next; }
	elsif ( $s eq '}') {
	    &printTable(\%h);
	    return;
	} elsif ($s =~ /^(\S+?)=(.*)/) {
	    &insert(\%h, $1,$2);
	} else {
	    die "Cannot parse the following line as name=val (run context):  $s\n";
	}
    }
    print STDERR "File ended before \@class closed\n";
}

#------------------------------------------------------------------
#-- removes leading and trailing white space, and trailing comments
sub trim($) {
    my ($s) = @_;
    $s=~ s/^\s+//;
    $s=~ s/\s+$//;
    $s=~ s/\#.*//;
    return $s;
}

sub createHash() {
    my %h = map {($_ => "")} @legalNames;
    return \%h;
}

sub copyHash($) {
    my ($pa)=@_;
    my %a=%$pa;
    my %h =();
    foreach my $x (keys %a) {
	$h{$x} = $a{$x};
    }
    return \%h;
}

sub insert($$$) {
    my ($pa, $name,$val) = @_;
    if (!defined ${$pa}{$name}) {
	die "Variable name '$name' is not listed as a legal column name\n";
    }
    ${$pa}{ $name } = $val;
}


sub printTable($) {
    my ($pa)=@_;
    my %h=%$pa;
    my $out="";

    foreach my $name (@legalNames) {
	if (length($out)>0) {
	    $out .= ",";
	}
	my $val = $h{$name};
	(defined $val) or $val='';
	$out .= '"' .$val. '"';
    }
    print "$out\n";
}

sub printHeader() {
   my $out="";
   foreach my $name (@legalNames) {
       if (length($out)>0) {
	   $out .= ",";
       }
       $out .= '"' .$name. '"';
   } 
   print "$out\n";
}
