#-- this file is included (with "source") into other shell scripts

#set driver=boxer.driver.Driver
set driver=edu.dimacs.mms.borj.Driver
set main=${home}/boxer
set cp=${main}/classes:${main}/lib/xercesImpl.jar
set opt="-Xmx256m -cp ${cp}"

#set d=${main}/rcv-out
set d=.
set out=${main}/out

if (-e $out) then
   echo Reusing directory $out
else
   echo Creating directory $out
   mkdir $out
endif

