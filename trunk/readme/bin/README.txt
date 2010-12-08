THIS IS A README.txt FILE INCLUDED WITH THE "BINARY DISTRIBUTION"
use-boxer-and-borj-*.*.*.zip

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
If you don't know what it is, please view  the documentation in
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

------------------------------------------------------------------

   --High level history of releases, and known bugs in current version.

 0.6.003 - late May 2009
    0.6.004 - 2009-06-18. Borj: respect quotes in cmd line
    0.6.005 - 2009-06-19. Boxer: introduced (and use) BoxerXMLException 
    0.6.006 - 2009-06-24. Boxer: improved DataPoint name management; 
       ParseXML enhancements (in connection with standard-scenarios.html)
    0.6.007 - 2009-06-28. Only updated docs

    0.7.001 - 2009-09-24. New ID validation process. Using caret instead of
       colon for compact XML format. New package tree structure.
    0.7.002 - 2009-09-28. Lazy truncation made correct in TruncatedGradient, and disabled in ExponentiatedGradient
    0.7.003 - 2009-12-08. Test BOXER applications added in edu.dimacs.mms.applications.learning
    0.7.004 - 2009-12-18. Enabled "physical" truncation 
    0.7.005 - 2010-02-18. New installation instructions; moving away RDF converters (to avoid using Jena)
    0.7.006 - 2010-04-19. The semantics of the "name" attribute of the "learner" element is now shifted to the "algorithm" attribute, while "name" has now a new independent meaning. The new (4th) column in the score files.

    0.8.000 - 2010-07-05. Transitionary version to the new "individual priors". I/O for individual priors has been added, but not the actual arithmetic yet
    0.8.001 - 2010-07-07. The individual priors appear to work, although more testing and documenting is still upcoming
    0.8.002 - 2010-12-07. Rearranged location of some files and directories. More experimental tools


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
