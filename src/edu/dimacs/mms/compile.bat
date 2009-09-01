set JAVA_HOME="C:\Program Files\Java\jdk1.6.0_14"
echo JH is %JAVA_HOME%
set cp=../classes;../lib/xercesImpl.jar;../lib/serializer.jar 

mkdir ..\classes

%JAVA_HOME%\bin\javac -Xlint:unchecked -classpath %cp% -d ../classes boxer/*.java borj/*.java borj/rcv/*.java tokenizer/*.java

