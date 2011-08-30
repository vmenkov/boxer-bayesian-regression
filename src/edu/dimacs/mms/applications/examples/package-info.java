/**    This package/directory contains a set of simple Java
       applications that use BOXER.  They are highly commented in
       order to help users who are unfamiliar with both BOXER and Java
       to quickly learn how to make basic use of BOXER.  As you learn
       more about Java and BOXER you will likely find the comments
       here and in the sample applications annoyingly overdetailed! 

<h3>
       1. The SimpleTrain_* Applications
</h3>

<p>
       Each application with a name of the form SimpleTrain_* uses the
       BOXER API to train a two-class PLRM (polytomous logistic
       regression model) on labeled data.  The default training data
       for each of these applications is the same, as is the choice of
       learning algorithm and its settings.  So by default each of
       these applications produces as output what is mathematically
       the same logistic regression model. What the different
       applications show off is how the same BOXER learning
       capabilities can be accessed through a variety of different
       input and output methods.

<p>
       Where possible each application uses the same format (e.g. a
       string, a file, etc.)  for both input of the training data and
       output of the trained model.  In real applications one could of
       course mix the API styles shown in the SimpleTrain_*
       applications, for instance supplying the training data as an
       in-memory string, but writing the trained PLRM directly to a
       file.

<p>
       In addition to producing the trained logistic regression model
       in an application-specific format, each of the applications
       also produces the Suite containing the trained model (as well
       as other information) in BOXER's standard XML format.  The XML
       output for the different applications can be compared to see
       that the same result is produced in all cases (with one
       exception noted in SimpleTrain_BBRfiles.java).

<p>
       If you would like to verify that the same Suite is produced in 
       all cases, open a command line window and change your
       working directory to 
<pre>
            .../boxer-bayesian-regression/sample-data/SimpleTrain
</pre>
       where boxer-bayesian-regression is the directory where the
       BOXER distribution has been installed.  The above directory
       contains the same training data file in different formats. Then
       issue these six commands
<pre>
            java -cp DIR edu.dimacs.mms.applications.examples.SimpleTrain_BOXERfiles
            java -cp DIR edu.dimacs.mms.applications.examples.SimpleTrain_DOM
            java -cp DIR edu.dimacs.mms.applications.examples.SimpleTrain_DataPoints
            java -cp DIR edu.dimacs.mms.applications.examples.SimpleTrain_strings
            java -cp DIR edu.dimacs.mms.applications.examples.SimpleTrain_BBRfiles
            java -cp DIR edu.dimacs.mms.applications.examples.SimpleTrain_BXRfiles
</pre>
       where DIR is the directory in which you have installed the 
       BOXER source.  DIR should contain only a directory named "edu". 
       Alternately you can add DIR to the appropriate environment 
       variable for the Java classpath on your system. 

<p>
       In the first four cases, the output (which you can see on the
       screen, or redirect to a file) is exactly the same: the BOXER
       XML representation of a trained Suite.  For
       SimpleTrain_BBRfiles (or SimpleTrain_BXRfiles), the output
       contains both a representation of the trained logistic
       regression model in BBR (or BXR) format, as well as the BOXER
       representation of the Suite.  

<p>
       In the case of BBR, the Suite representation uses integer class
       names and feature IDs, because the BBR format for training data
       files does not support symbolic names.  Otherwise the
       Suite representation is identical to the other cases.

<p>
       None of the applications use a Suite definition or Prior
       definition as a starting point.  Almost all aspects of learning
       therefore occur as specified by the BOXER defaults, and the
       nature of the discrimination is inferred from the labels on the
       training data.  

*/

package edu.dimacs.mms.applications.examples;

/*
Copyright 2011, Rutgers University, New Brunswick, NJ.

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
CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.  */
