#-- This file is included (with the "source") into shell scripts located in the subdirectories of this
#-- directory. It sets a few shell variables:
#
#  driver = the class name for BORJ main class
#  out    = the output directory for this script
#  opt    = Java options (classpath, memory size)
#  subdir = name of the subdirectory of the "scripts" directory from where the particular script is running
#  d      = name of the directory from which this script should read data set files
#  s      = name of the directory from which this script should read suite files



#set driver=boxer.driver.Driver
set driver=edu.dimacs.mms.borj.Driver



#set main=${home}/boxer
#set d=${main}/rcv-out
#set d=.
#set out=${main}/out

#-- The script findBoxerJar.pl will try to find the location of
#-- boxer.jar (form the environment variable BOXER_JAR_PATH, or by
#-- looking at a few likely locations). If it fails to do so, or if
#-- you have multiple jar files with that name and want to make sure
#-- that the right one is used, you may either set the environment
#-- variable BOXER_JAR_PATH, or add boxer.jar (at its actual location)
#-- to your CLASSPATH

set cp=`../findBoxerJar.pl`

#-- Setting JVM options (memory used, classpath)
set opt="-Xmx256m"
if ("$cp" != "") then
  set opt="${opt} -cp ${cp}"
else
  echo "Location of boxer.jar is not known."
endif

#-- Where are we running this script in?
set startdir=`pwd`
set subdir=`basename $startdir`
#echo "Sub=${subdir}"

#-- The directory into which the output will go
#-- You may want to modify this setting as suits you.

set outroot=../../new-output

if (! -e $outroot) then 
    mkdir $outroot
endif

set out=$outroot/$subdir

if (-e $out) then
   echo Reusing output directory $out
else
   echo Creating output directory $out
   mkdir $out
endif

set d=../../datasets/$subdir
set s=../../suites/$subdir
