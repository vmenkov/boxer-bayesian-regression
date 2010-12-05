#!/bin/csh

#-- This script is supposed to show that all BOXER's components are
#-- installed and working. The script is supposed to work in both of
#-- the following situations: (a) when its directory (tiny-sample) is
#-- a coordinate directory with "lib", and thus the necessary jar file
#-- can be found in ../lib/boxer.jar; (b) when its directory
#-- (tiny-sample) is a subdirectory of a directory co-ordinate with
#-- "lib", and thus the necessary jar file can be found in
#-- ../../lib/boxer.jar. If the location of the JAR file different,
#-- you need to change the classpath accordingly (e.g. use the
#-- absolute path), so that boxer.jar at its proper location will show
#-- up there

java -cp ../lib/boxer.jar:../../lib/boxer.jar edu.dimacs.mms.borj.Driver train:train-set.xml test:test-set.xml
