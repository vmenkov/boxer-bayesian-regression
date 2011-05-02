use strict;

#--   ($logFile,$logDir) = &validateLogFileName( $fileOrDirPath );
#--    read from <F> ....
sub validateLogFileName($) {
    my ($q) = @_;
    my ($logFile,$logDir)=(undef,undef);

    if (-d $q) {
	#-- well, it's a directory... let's look for the log file there
	$logDir=$q;
	opendir(my $dh, $logDir) or die "can't open directory $logDir: $!";
	my @dAll =  readdir($dh);
	my @dFiles = grep { -f "$logDir/$_" && ($_ =~ /\.log$/ || $_ =~ /\.log\.gz$/)} @dAll;
	my $n=scalar(@dFiles);
	if ($n==0) {
	    die "There is no log file in directory $logDir\n";
	} elsif ($n==1) {
	    #-- ok
	    $logFile = "$logDir/" . $dFiles[0];
	} elsif ($n==2) {
	    if ($dFiles[0] eq $dFiles[1].".gz") {
		 $logFile = "$logDir/" . $dFiles[1];
	    } elsif  ($dFiles[1] eq $dFiles[0].".gz") {
		 $logFile = "$logDir/" . $dFiles[0];
	    } else {
		die "Cannot figure which of ".join(", ", @dFiles)." is the correct log file in directory $logDir\n";
	    }
	} else {
	    die "Cannot figure where is the log file in directory $logDir\n";
	}
    } elsif (-f $q || -f "${q}.gz") {
	$logFile = $q;
	$logDir = ($logFile =~ /(.*\/)/) ? $1 : ".";
    } else {
	die "There is neither a file named  $q or ${q}.gz, nor a directory named $q\n";
    }

    if (-e $logFile) {
	open(F, $logFile) or die "Cannot read log file $logFile\n";
	print STDERR "Reading $logFile\n";
    } elsif (-e "${logFile}.gz") {
	open(F, "zcat ${logFile}.gz |") or die "Cannot run zcat ${logFile}.gz\n";
	print STDERR "zcat ${logFile}.gz\n";
    } else {
	die "No file named $logFile or ${logFile}.gz can be found\n";
    }
    return ($logFile,$logDir);
}


1;
