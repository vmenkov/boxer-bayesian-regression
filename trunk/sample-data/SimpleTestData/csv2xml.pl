#!/usr/bin/perl

use strict;

print "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<dataset name=\"SimpleTestData\" boxerversion=\"0.7\">
";

my $s = undef;

my $cnt=0;
while(defined ($s=<>)) {
    $s =~ s/^\s+//;
    $s =~ s/\s+$//;
    if ($s eq '') { next; }
    my ($id, $label0, @a) = split(",\s*", $s);
    my $label = $label0;
    if ($label > 3) { $label = 1;}

    for(my $i=0; $i<3; $i++) {
	if ($a[$i] > $a[$label-1]) {
	    $label = $i+1;
	}
    }   

    if ($label != $label0) {
	print STDERR "$id: replaced $label0 with $label!  Row=$s\n";
    }

    print "<datapoint name=\"$id\">
   <labels>PK^$label</labels>
   <features>";
    for(my $i=0; $i<4; $i++) {
	print ($i+1);
	print "^$a[$i] ";
    }
    print "</features>\n";
    print "</datapoint>\n";    
}

print "</dataset>\n";
