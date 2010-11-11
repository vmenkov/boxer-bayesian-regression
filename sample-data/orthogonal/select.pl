#!/usr/bin/perl

#-- This is an auxiliary file used to parse the log file with the
#-- results of a Repeater run, and to produce input files for gnuplot


use strict;


sub usage($) {
    my ($msg) = @_;
    print "Usage:
  $0 dir
  $0 input-file output-file
";
    die $msg;
}

# print join ("\n", @ARGV). "\n";


($#ARGV >= 0) or &usage( "No arg");

my ($x) = $ARGV[0];

my ($file, $dir);

if (-d $x) {
    $dir = $x;
    $file = "$x/run.log";
} elsif (-f $x) {
    $file = $x;
    $dir = ( $file =~ /^(.*)\//)  ? $1 : ".";
} else {
    &usage("$x is neither a file nor directory");
}


my $out = ($#ARGV >=1) ? $ARGV[1] : "$dir/run.dat";
(defined $out) or  &usage("Output not specified");


print "Parsing $file, output to $out\n";

open(F, $file) or die "Can't read file $file";

my @sections = ();

my $s=undef;
while(defined ($s=<F>)) {    
    if ($s=~/\[(TRAIN|TEST) (LOG LIN|WARECALL)\]\[.*.xml: *(\-?\d+) *: *(\d+)\+\d+\]\[PK\]/ ) {
	my $j = $3;
	if ($j==-1) { $j=0; } #-- special case, only one run here
	if (!defined $sections[$j]) { $sections[$j] = [$s]; }
	push @{$sections[$j]}, $s; 
    }
}
close(F);

my @sums = ();

for(my $j=0; $j <= $#sections; $j++) {
   # print "XX: section $j\n";
    my $jout = "$dir/run." . sprintf("%03d", $j) . ".dat";
    open(G, ">$jout") or die "Can't write to file $jout";

#-- 6 elements are stored in each row:
#    my @a=(); # train.Log train.Lin
#    my @b=(); # test.Log test.Lin
#    my @c=(); # train.WtAvgRecall
#    my @d=(); # test.WtAvgRecall
    my @data = ();


    my $ff= "[\d\.\+\-E]+";

    foreach my $s (@{$sections[$j]}) {
	if ($s=~/\[TRAIN LOG LIN\]\[.*.xml: *(\-?\d+) *: *(\d+)\+\d+\]\[PK\]\s+($ff)\s+($ff)/ ) {
	    @{$data[$2]}[0..1] = ($3, $4);
	}elsif ($s=~ /\[TEST LOG LIN\]\[.*.xml: *(\-?\d+) *: *(\d+)\+\d+\]\[PK\]\s+($ff)\s+($ff)/ ) {
	    @{$data[$2]}[2..3] = ($3, $4);
	}elsif ($s=~ /\[TRAIN WARECALL]\[.*.xml: *(\-?\d+) *: *(\d+)\+\d+\]\[PK\]\s+($ff)/) {
	    ${$data[$2]}[4] = $3;
	}elsif ($s=~ /\[TEST WARECALL]\[.*.xml: *(\-?\d+) *: *(\d+)\+\d+\]\[PK\]\s*($ff)/) {
	    ${$data[$2]}[5] = $3;
	}
    }

    for(my $i=1; $i<= $#data; $i++) {
	if (defined $data[$i]) {
	    my @q = @{$data[$i]};
	    if (defined $q[0]) {
		foreach my $k (0..5) {
		    (defined $q[$k]) or die "Missing entry for i=$i, k=$k";
		    ${$sums[$i]}[$k] += $q[$k];
		}
		print G "$i\t". join("\t", @q) . "\n";
	    }
	}
    }
    close(G);
}

#-- averages
my $n =  $#sections +1;
for(my $i=1; $i<= $#sums; $i++) {
    if (defined $sums[$i]) {
	foreach my $k (0..5) {
	    ${$sums[$i]}[$k] /= $n;
	}
    }
}

open(G, ">$out") or die "Can't write to file $out";

for(my $i=1; $i<= $#sums; $i++) {
    if (defined $sums[$i]) {
	my @q = @{$sums[$i]};
	print G "$i\t". join("\t", @q) . "\n";
    }
}
close(G);



