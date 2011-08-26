THIS IS A README.txt FILE INCLUDED WITH THE "SOURCE CODE DISTRIBUTION"
boxer-and-borj-source-*.*.*.zip

This distribution includes the Java source code and files needed to
compile it to a Jar file.




 == What BOXER and BORJ are ==

BOXER is a Java library for online anytime machine learning of
polytomous logistic regression models. It uses XML as the main format
for describing input data, but it is possible for the user to use API
in other ways as well. You can use the BOXER API in your own Java
applications, or you can use several BOXER applications (standalone
Java programs) included in this distribution.

One of the BOXER applications included in this distribution is BORJ.
BORJ is standalone command-line driven Java program that runs machine
learning experiments using BOXER.

A number of other small BOXER applications (Repeater, WindowRepeater)
are included in this distribution as well.

To use BOXER and BOXER applications you need to have a Java virtual
machine (e.g., in the form of Sun's JRE) installed on your computer.
If you don't know what a JRE is, please view  the documentation in
the file doc/html/installation.html  with your web browser.

For more details on BOXER, BORJ, and other applications, please view
the documentation in the directory doc/html with your web browser.

 == Files in this distribution ==

1a. README.txt - this file
1b. INSTALL.txt : Installation and configuration instructions
1c. USAGE.txt : Basics of how to use BORJ, basic troubleshooting, and
pointer to more detailed documentation

2. lib/boxer.jar : JAR file containing bytecode for BOXER, and BOXER
applications (BORJ and others)

3. lib/xercecImpl.jar, lib/xml-apis.jar - The distribution may include
these files from the Apache Xerces project. These are third-party
libraries used by BOXER for parsing and generating XML. These
libraries are distirbuted under the Apache License. If these JAR files are
not included, please refer to the instructions in 

4. tiny-sample/ - Contains sample data files and a sample shell script for using BORJ

5. doc/ - Documentation for BOXER, BORJ, and other applications, in
HTML and PDF (to appear) formats


---------------------------------------------------------------
		TO ADD:

[What the different files in the distribution are (including brief
explanation of Java, JARs, etc.)]

.......

               --Why you should also download the sample data distribution
               --Where other documentation is
               --Authors and acknowledgment

........

   --High level history of releases, and known bugs in current version.


=============================================================

Copyright 2009, Rutgers University, New Brunswick, NJ.

All Rights Reserved

Permission to use, copy, and modify this software and its
documentation for any purpose other than its incorporation into a
commercial product is hereby granted without fee, provided that the
above copyright notice appears in all copies and that both that
copyright notice and this permission notice appear in supporting
documentation, and that the names of Rutgers University, DIMACS, and
the authors not be used in advertising or publicity pertaining to
distribution of the software without specific, written prior
permission.

RUTGERS UNIVERSITY, DIMACS, AND THE AUTHORS DISCLAIM ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR ANY PARTICULAR PURPOSE. IN NO EVENT
SHALL RUTGERS UNIVERSITY, DIMACS, OR THE AUTHORS BE LIABLE FOR ANY
SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER
RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF
CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
