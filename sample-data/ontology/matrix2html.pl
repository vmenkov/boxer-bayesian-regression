#!/usr/bin/perl -s

# This is a script to extract the confusion matrix from the output of BOA, and 
# to convert it to a human-readable HTML format
#-- Usage:
# java ... edu.dimacs.mms.applications.ontology.Driver train:... test:... > out.log
# matrix2html.pl [-diag=off|pos|names] out.log
#
# The -diag option controls what cells recognized as "diagonal" and painted yellow

use strict;

#require 'abbreviate_sub.pl';

#-- flags controllig how the diagonal blocks of the confusion matrix
#-- are identified
my ($diagPos,$diagNames) = (1,0);
if ($::diag eq 'off') {
    $diagPos=$diagNames=0;
} elsif  ($::diag eq 'pos') {
    $diagPos=1; $diagNames=0;
} elsif  ($::diag eq 'names'  || !defined $::diag) {
    $diagPos=0; $diagNames=1;
} else {
    die "Illegal option value: -diag=$::diag. Legal values are off|pos|names\n";
}

my @colNames=();
my $matNo=0;
my $inside = 0;

my $s;
my $rowCnt=0;
while(defined ($s = <>)) {
 
    if ($s =~ /^=+ Confusion matrix -(.*?)=*\s*$/) {
	my $title = $1;
	if ($inside) {
	    &finalizeFile();
	}

	$matNo++;
	my $fname="matrix-${matNo}.html";
	my $csvname="matrix-${matNo}.csv";
	open(G, ">$fname") or die "Cannot write to $fname\n";
	open(H, ">$csvname") or die "Cannot write to $csvname\n";
	print G "<html>\n" .
	    "<head><title>Confusion matrix - $title</title></head>\n" .
	    "<body>\n" .
	    "<h1>$title</h1>" .
	    "<table border=1>\n";
	$inside=1;
	$rowCnt=0;
	next;
    }

    if ($s !~ /^P\(.+?\)/) {
	if ($inside) {
	    &finalizeFile();
	    $inside=0;
	}
	next;
    }
    
    #-- We know that we've read a "P(...)" line; let's process it.

    if ($rowCnt==0) {
	&printHeader($s);
    }

    
    print G "<tr>\n";
    my $q=$s;
    my $colCnt=0;
    my @nums=();
    my @longnums=();
    my $rowName = "";
    while($s =~ /P\((.+?)\|(.+?)\)=([\d\.E+\-]+)/ig) {
	if ($colCnt==0) {
	    $rowName = $2;
	    print G "<th>$2</th>\n";
	}
	my $num = $3;
	push @longnums, $num;
	if ($num =~ /E-\d\d?$/) {
	    $num = 0;
	} else {
	    $num =~ s/^(\d\.\d\d\d)\d*/$1/;
	}
	$colCnt++;
	push @nums, $num;
    }


    #-- print "rowName=$rowName, base=$rowNameBase\n";

    print H join(",", map{ '"' . $_ . '"'; } @longnums) . "\n";

    #-- Find top values, using desc sort
    my @indexes = ($0..$#nums);
    @indexes = sort {$nums[$b]<=>$nums[$a]} @indexes;


    $colCnt=0;
    foreach my $num (@nums) {
	#-- 0=none, 1=light, 2=strong
	my $diag = 0;   
	if ($diagPos) {
	    $diag = ($colCnt==$rowCnt) ? 2 : 0;
	} elsif ($diagNames) {
	    $diag = &compareNames($rowName, @colNames[$colCnt]);
	}
	    


	my $att = ($diag==0) ? '' :
	    ' bgcolor="'. ($diag==2 ? "#ffff90" : "#ffffd8") . '"';
	my $text = $num;
	if ($num >= 0.1) {
	    $text = &wrap($text, "i");
	}
	if ($colCnt == $indexes[0]) {
	    $text = &wrap($text, "strong");
	}
	print G "<td${att}>${text}</td>\n";	
	$colCnt++;
    }

    print G  "</tr>\n";
    $rowCnt++;
}

#-- Appends a modifier element before and after a given text
sub wrap($$) {
    my ($text, $mod) = @_;
    return "<${mod}>${text}</${mod}>";
}


sub finalizeFile() {
    print G  "</table>\n" .
	 "</body></html>\n";
    close(G);
    close(H);
}


#-- Takes a list of table column names, and prepares a well-formatted
#-- and more or less compact human-readble table header.
sub printHeader {
    my ($s) = @_;
    my @w=();
    my $csv="";
    @colNames = ();
    while($s =~ /P\((.+?)\|(.+?)\)=([\d\.E+\-]+)/ig) {
	my $name = $1;

	push @colNames, $name; #-- original name

	$name = &abbreviate($name);
	push @w, $name; #-- abbreviated printable name
	if ($csv ne '') { $csv .= ",";}
	$csv .= '"' . $name . '"';
    }

    print H "$csv\n";

    my $out1 = "<tr><th rowspan=2>New DS \\ Old DS</th>\n";
    my $out2 = "<tr>\n";

    for(my $i=0; $i <= $#w; $i++) {
	if ($w[$i] =~ /(.*)(\d+)$/) {
	    my ($base, $num) = ($1, $2);
	    $out2 .= "<th>$num</th>\n";    
	    my $cnt=1;
	    while( $i+1 <= $#w && $w[$i+1] =~ /(.*)(\d+)$/ && $1 eq $base) {
		$i++;
		$cnt++;
		$out2 .= "<th>$2</th>\n";    	 	
	    }
	    $out1 .= "<th colspan=$cnt>$base</th>\n";
	} else {
	    $out1 .= "<th rowspan=2>$w[$i]</th>\n";
	}
    }

    $out1 .= "</tr>\n";
    $out2 .= "</tr>\n";
    print G $out1 . $out2;
}

#-- returns 2 if the names are equal or "likely equal"; return 1 if
#-- they only differ in index
sub compareNames($$) {
    my ($a,$b) = @_;
    if ($a eq $b) {return 2; }
    my ($base1, $suff1) = &analyzeName($a);
    my ($base2, $suff2) = &analyzeName($b);
    if ($base1 eq $base2) {
	return ($suff1 eq $suff2 ||
		$suff1 eq '' && $suff2 eq '1' ||
		$suff2 eq '' && $suff1 eq '1' )  ? 2: 1;
    } else {
	return 0;
    }
}

#-- ($base, $suff) = ...
sub analyzeName($) {
    my ($base) = @_;
    my $suff = '';
    if ($base =~ /^(.*?)(\d+)$/) {
	($base, $suff) = ($1,$2);
    }
    # FooList_Foo[Something] --> FooSomething
    if ($base =~ /^(.*)List_(.*)$/) {
	my ($pref,  $rest) = ($1,$2);
	if (substr($rest,0, length($pref)) eq $pref) {
	    $base = $rest;
	}
    }
    return ($base, $suff);
    
}

sub abbreviate($) {
    my ($name)=@_;
    $name =~ s/([a-z_])([A-Z])/$1 $2/g;
    return $name;
}
