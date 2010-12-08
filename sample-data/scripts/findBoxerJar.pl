#!/usr/bin/perl
use strict;

#--------------------------------------------------------------------------
#--- Now we will be looking for boxer.jar, in order to set $BOXER_JAR_PATH,
#--- if that variable has not been set yet. The expected location is something
#--- like use-boxer-*/lib/boxer.jar, or boxer-*/lib/boxer.jar; but we don't
#--- know how far up the directory tree that place may be located!
#---------------------------------------------------------------------------


if (defined $ENV{'BOXER_JAR_PATH'}) {
	print $ENV{'BOXER_JAR_PATH'};
} else {
	print &lookForBoxerJar();
}
print "\n";

#-- Looks for boxer.jar at various likely locations, and returns an
#-- absolute path to it. If can't find it, returns an empty string

sub lookForBoxerJar() {
    my $base='boxer.jar';
    my $maxCnt=7;
    
    for(my $cnt=0; $cnt<$maxCnt; $cnt ++) {
	my $root = `pwd`;
	$root =~ s/\s+$//;
	(-r $root) and (-x $root) or     next; 
	foreach my $test ("$root/$base",  "$root/lib/$base") {
	    if (-e $test) { return $test; }
	}
	
	opendir(my $dh, $root) || die "can't opendir $root: $!";
	my @subdirs = grep { /boxer/ && -d "$root/$_" } readdir($dh);
	closedir $dh;
	
	foreach my $s (@subdirs) {
	    my $loc = "$root/$s";
	    (-r $loc) or     next; 
	    foreach my $test ("$loc/$base",  "$loc/lib/$base") {
		if (-e $test) { return $test; }
	    }
        }

	($root ne '/') and chdir( '..') or last;   
     }
    return '';
}

