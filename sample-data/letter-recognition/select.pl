#!/usr/bin/perl
use strict;

sub usage($) {
    my ($msg) = @_;
    print "Usage:
  $0 dir
  $0 input-file output-file [output-file-sd]
";
    die $msg;
}

# print join ("\n", @ARGV). "\n";


($#ARGV >= 0) or &usage( "No arg");

my ($x) = $ARGV[0];

my ($file, $zfile, $dir);


if (-d $x) {
    $dir = $x;
    $file = "$x/run.log";
} else {
    $file = $x;
    $dir = ( $file =~ /^(.*)\//)  ? $1 : ".";
}

if (-f $file) {
    print "Found file $file\n";
} elsif (-f "$file.gz") {
    $zfile = "$file.gz";
    print "Found file $zfile\n";
} else {
    &usage("$x is neither a file nor directory, nor is there $x.gz");
}


my $out = ($#ARGV >=1) ? $ARGV[1] : "$dir/run.dat";
(defined $out) or  &usage("Output (MAIN) not specified");

my $out2 = ($#ARGV >=2) ? $ARGV[2] : "$dir/run-sd.dat";
(defined $out2) or  &usage("Output (SD) not specified");

my $dis = "letter";

print "Parsing $file, output to $out\n";

if (defined $zfile) {
    open(F, "zcat $zfile|") or  die "Can't read (with zcat) file $zfile";
} else {
    open(F, $file) or die "Can't read file $file";
}

#-- read the log files, and extract relevant lines, placing them into @sections
my @sections = ();
my $only = undef;
my $s=undef;
while(defined ($s=<F>)) {    
    if ($s=~/\[(TRAIN|TEST) (LOG LIN|WARECALL)\]\[.*.xml: *([\-\d]+) *: *(\d+)\+\d+\]\[$dis\]/ ) {
	my $j = $3;
	if ($j>=0) {
	    if (!defined $sections[$j]) { $sections[$j] = [$s]; }
	    push @{$sections[$j]}, $s; 
	} elsif ($j==-1) {
	    if (!defined $only) { $only = [$s]; }
	    push @{$only}, $s; 
	}
    }
}
close(F);

if (defined $only) {
    ($#sections == -1) or "Can't have both numbered and number-less sections";
    $sections[0] = $only;
    print "Using number-less section as sec. no. 0\n";
} else {
    ($#sections >= 0) or die "No sections found";
}

printf "Will average sections 0..$#sections\n";

my @sums = ();
my @sumSqs = ();

for(my $j=0; $j <= $#sections; $j++) {
    my $jout = "$dir/run." . sprintf("%03d", $j) . ".dat";
    open(G, ">$jout") or die "Can't write to file $jout";

#--  the argument n, and 6 elements are stored in each row:
#    my @a=(); # train.Log train.Lin
#    my @b=(); # test.Log test.Lin
#    my @c=(); # train.WtAvgRecall
#    my @d=(); # test.WtAvgRecall
    my @data = ();


    my $ff= "[\d\.\+\-E]+";

    foreach my $s (@{$sections[$j]}) {
	if ($s=~/\[TRAIN LOG LIN\]\[.*.xml: *([\-\d]+) *: *(\d+)\+\d+\]\[$dis\]\s+($ff)\s+($ff)/ ) {
	    @{$data[$2]}[0..1] = ($3, $4);
	}elsif ($s=~ /\[TEST LOG LIN\]\[.*.xml: *([\-\d]+) *: *(\d+)\+\d+\]\[$dis\]\s+($ff)\s+($ff)/ ) {
	    @{$data[$2]}[2..3] = ($3, $4);
	}elsif ($s=~ /\[TRAIN WARECALL]\[.*.xml: *([\-\d]+) *: *(\d+)\+\d+\]\[$dis\]\s+($ff)/) {
	    ${$data[$2]}[4] = $3;
	}elsif ($s=~ /\[TEST WARECALL]\[.*.xml: *([\-\d]+) *: *(\d+)\+\d+\]\[$dis\]\s*($ff)/) {
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
		    ${$sumSqs[$i]}[$k] += $q[$k]*$q[$k];
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
	    ${$sumSqs[$i]}[$k] /= $n;
	}
    }
}

open(G, ">$out") or die "Can't write to file $out";
open(H, ">$out2") or die "Can't write to file $out2";

for(my $i=1; $i<= $#sums; $i++) {
    if (defined $sums[$i]) {
	my @x = @{$sums[$i]};
	my @y = @{$sumSqs[$i]};

	#-- print averages
	print G "$i\t". join("\t", @x) . "\n";

	#-- print averages and standard deviations
	my @q = ();
	for(my $j=0; $j<= $#x; $j++) {
	    my $d = $y[$j] -$x[$j]*$x[$j];
	    if ($d < 0) {
		#-- arithmetic error?
		if (abs($d) < 1e-8 * $y[$j]) { $d = 0; }
		else {
		    die "Trouble with sd: sum(x^2)=$y[$j], (sum(x)^2)=".($x[$j]*$x[$j]).", d=$d";
		}
	    }
	    push @q, $x[$j], sqrt( $d);
	}
	print H "$i\t". join("\t", @q) . "\n";       
    }
}
close(G);
close(H);



