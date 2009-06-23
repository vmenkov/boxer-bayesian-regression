
set ver=UNKNOWN

set cp=../classes;../lib/xercesImpl.jar;../lib/serializer.jar 

set d=..\javadoc

mkdir %d%

rem Copy some XML files that you want to become part of the documentation

copy sample-data\sample*.xml boxer\doc-files
copy sample-data\eg-learner-param-1.xml boxer\doc-files

rem  -use -link http://java.sun.com/j2se/1.5.0/docs/api/ -header "<em>BOXER ${ver}</em>" 

javadoc -protected -d %d% -sourcepath . -classpath %cp% -header "<em>BOXER - MS Windows</em>" -windowtitle "BOXER API" -overview overview.html  boxer borj.rcv borj tokenizer


