#!/usr/bin/perl -s

#-- prepares a master file (for the TI task) from a data file that looks like this:

#./w0-g0/gazetteer.log
#10971/10971
#0/2200
#0.8329663654999621

use strict;

print '@series{' . "\n";
print "CaseName=GazetteerAllen20110504\n";
print "Learner=ASD\n";
print "Date=May 04, 2011\n";

my $s=undef;

my $cnt=0;
while(defined ($s=<>)) {
    $cnt++;
    $s =~ s/\s+$//;
    ($s =~ /gazetteer.log/) or die "Unexpected content in line no. $cnt: $s\n";
    my $rep= ($s =~ m|/(w.-g.)/|) ? $1 : "";
    
    my $log = $s;
    my @a=();
    my @truePos=();
    my @dataPos=();
    foreach my $i (0..1) {
	$s = <>;
	defined $s or  die "Unexpected end of file, line no. $cnt\n";
	$cnt++;
	$s =~ s/\s+$//;	
	push @a, $s;
	($s =~ m|^(\d+)/(\d+)\s*$|) or die "Can't parse line no. $cnt: $s\n";
	push @truePos, $1;
	push @dataPos, $2;
    }
    $s = <>;

    print '@run{' . "\n";
    print "RunLog=$log\n";
    print "Representation=$rep\n";

    my $setSize = $dataPos[0] + $dataPos[1];
    my $correctlyPredicted = $truePos[0] + $truePos[1];

    #-- Label^person
    #-- Label^place
    my @classnames=('person', 'place');
    foreach my $i (0..1) {
	print '@class{' . "\n";
	print "DiscriminationName=Label\n";
	print "ClassName=$classnames[$i]\n";

	my $dataN = $setSize - $dataPos[$i];
	my $iother = 1-$i;
	my $falsePos = $dataPos[$iother] - $truePos[$iother];

	my $detection = ($dataPos[$i]==0) ? 0 : $truePos[$i]/$dataPos[$i];
	my $falseAlarm =  ($dataN==0) ? 0 :$falsePos/$dataN;
	#-- as per Paul's message, "displaying via ROC", 2011-04-18
	my $roc = 0.5* (1+$detection - $falseAlarm);


	my $setLabel='TestSet';
	my @pairs2 =
	    (
	     "${setLabel}Positive_N=$dataPos[$i]",
	     "${setLabel}Complement_N=$dataN",
	     "${setLabel}TruePos_N=$truePos[$i]",
	     "${setLabel}FalsePos_N=$falsePos",
	     "${setLabel}Detection=$detection",
	     "${setLabel}FalseAlarm=$falseAlarm",
	     "${setLabel}ROC=$roc");

	print join("\n", @pairs2) . "\n";

	print '}' . "\n";


    }
    print '}' . "\n";

   
}
print '}' . "\n";
