rem This script runs BOXER on a small sample

set JAVA_HOME="C:\Program Files\Java\jdk1.6.0_14"
echo JH is %JAVA_HOME%


set driver=borj.Driver
set main=..\..
set cp=../../classes;../../lib/xercesImpl.jar;../../lib/serializer.jar 
set opt="-Xmx256m"

set out=%main%\out

set d=%main%\src\sample-data

rem set a=train:'%d%/train-set.xml'
set a=train:'C:\tmp\train-set.xml'
 
set a1=test:'C:\tmp\train-set.xml':%out%/sample-aa-scores-tg.txt 
set b=test:'C:\tmp\test-set.xml':'C:\tmp\sample-ab-scores-tg.txt' 

echo a is %a%

java %opt% -cp %cp% -Dmodel=tg %driver% %a% %a1% %b% write-suite:%out%/sample-suite-out-x.xml write:%out%/sample-model-x.xml >%out%\windows2.tmp


