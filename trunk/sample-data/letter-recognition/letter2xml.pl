#!/usr/bin/perl

#-- This script converts data from
#-- http://archive.ics.uci.edu/ml/machine-learning-databases/letter-recognition/
#-- to the compact XML format used by Boxer

use strict;

my @letters = qw/A B C D E F G H I J K L M N O P Q R S T U V W X Y Z/;
my %letterSet = ();
foreach my $a (@letters) { $letterSet{$a} = 1; };

print "Keys = " . join("; ", keys %letterSet)."\n";

my $M = 16;
my $sep="^";

my $fname = "letter-recognition.data";
my $gname = "letter-recognition.xml";
my $sname = "letter-recognition-suite.xml";
open(F, $fname) or die "Cannot read $fname";

print "Reading $fname, writing to $gname\n";
open(G, ">$gname") or die "Cannot write to $gname";

my $s = undef;

print G "<dataset name=\"LetterRecognition\">\n";

my $cnt=0;
while(defined ($s=<F>)) {
    $cnt++;
    $s =~ s/\s*$//;
    my @q = split(",", $s);
    my $n = $#q + 1;
    ($n==$M+1) or die "Invalid line length ($n tokens) for line: $s";
    printf G "<datapoint name=\"%05d\">\n", $cnt;
    print G "<labels>letter${sep}$q[0]</labels>\n<features>";
    foreach my $j (1..$M) {
	print G " ${j}${sep}$q[$j]";
    }
    print G "</features>\n</datapoint>\n";    
}


print G "</dataset>\n";

close(G);

open(G, ">$sname") or die "Cannot write to $sname";
print G "<suite name=\"LetterRecognition\">\n";
print G "<discrimination name=\"letter\">\n<classes>\n";
print G join(" " , @letters);
print G "\n</classes>\n</discrimination>\n";
print G "</suite>\n";
close(G);
