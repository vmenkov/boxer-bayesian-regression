/**
BOXER, a Java library for online anytime machine learning of polytomous logistic regression models.

The BOXER (standing, more or less, for Bayesian Online EXtensible
Regression) library supports online, anytime fitting of polytomous
logistic regression models, as well as their use as multiclass
classifiers.  We attempt to summarize here the major characteristics
of BOXER from three perspectives:

<ul>
<li>
<strong>Scientific:</strong> Mathematical and algorithmic issues in
BOXER, and where it sits in the context of research on machine
learning.

<li>
<strong>Functional:</strong> BOXER considered from a user's
standpoint.  The emphasis is on using BOXER in its current form, via
existing APIs and input/output formats, to accomplish some task.

<li>
<strong>Architectural:</strong> This aspect of the documentation
focuses on how BOXER is implemented.  It is largely relevant to those
maintaining or modifying BOXER.
</ul>

<h2>Scientific Perspective:</h2> 

<p> The algorithms implemented in
BOXER include versions of the {@link
edu.dimacs.mms.boxer.ExponentiatedGradient Exponentiated Gradient}
(EG), stochastic gradient (SG), and {@link
edu.dimacs.mms.boxer.TruncatedGradient Truncated Gradient} (TG)
algorithms.  All are conventionally viewed as algorithms for "online
learning".  BOXER has its origins in our work on teasing apart and
rationalizing the multiple and unclear goals that have been pursued
under the heading of online learning.

<p>BOXER's TruncatedGradient may also be enhanced with individual
priors, which will allow the user to affect the behavior of PLRM
matrix coefficients during learning.

<h2>Functional Perspective:</h2>

<p>
BOXER supports machine learning of polytomous logistic regression
models (PLRMs) from labeled training data, as well as applying such
models to new data to solve binary or multiclass classification
problems.  Notable characteristics of BOXER include:
<ul>
<li>BOXER obeys configurable limits on memory and compute time when
performing its operations.
 
<li>BOXER's algorithms are anytime algorithms.  At any point during
processing, BOXER can be told to output a PLRM based on the training
data processed so far.

<li>BOXER's state can be serialized and deserialized.

<li>BOXER can take advantage of sparsity in feature values, class
labels, and model coefficients.

<li>BOXER can use domain knowledge in the form of Bayesian priors to
reduce the need for labeled training data [NOT YET IMPLEMENTED].

<li>BOXER can simultaneously train models for multiple multiclass
discriminations.

<li>Data and models are read and written in XML, with arbitrary names
allowed for features, classes, and tasks.
</ul>

<p>
The BOXER distribution also includes a standalone program, {@link edu.dimacs.mms.borj
BORJ}, which may be used for running machine learning experiments with
the BOXER library.  Users interested in carrying out machine learning
experiments with BOXER should begin by looking at the BORJ
documentation.

<h2>Architectural perspective:</h2>

<p> BOXER is a Java library meant to be used in building applications,
rather than being a standalone application itself.  
<!-- The BOXER API is
described at ______ [DL to VM: or do we describe it here on this
page?].  From a design pattern standpoint, BOXER is similar to ____.
-->

<p>
In exploring the BOXER API, we suggest first examining these major classes:

<ul> 

<li> {@link edu.dimacs.mms.boxer.DataPoint DataPoint}: A DataPoint represents a
single entity of interest.  The content of a DataPoint consists in the
values it takes on for various features, and in the labels that
indicate to what class the point belongs with respect to various
discriminations.  BOXER reads (and writes) DataPoints in XML form.

<li>{@link edu.dimacs.mms.boxer.Discrimination Discrimination}: A Discrimination is a
set of mutually exclusive and collectively exhaustive classes.  If a
Discrimination applies to a particular DataPoint, the DataPoint
logically must belong to exactly one of the Classes in that
Discrimination, even if that Class is not currently known.

<li>{@link edu.dimacs.mms.boxer.Suite Suite}: A set of Discriminations.  A Boxer's
Learner works with exactly one Suite.  A Suite can consist
of related {@link edu.dimacs.mms.boxer.Discrimination discriminations} (e.g. four binary
discriminations, one each of the news categories Politics, Sports,
Financial, and Weather), unrelated ones (e.g. one multiclass
discrimination for geographic region, and another multiclass
discrimination for news topic), or both.

<li>{@link edu.dimacs.mms.boxer.Priors Priors}: A set of priors can be
optionally associated with a Suite. It will affect the behavior of all
{@link  edu.dimacs.mms.boxer.TruncatedGradient TruncatedGradient} learners on that suite.

<li>{@link edu.dimacs.mms.boxer.Learner Learner}: Each concrete
class extending the abstract Learner class implements a
particular algorithm for producing classifiying models (e.g. PLRMs),
given prior knowledge, a discrimination definition, and a series of
training examples. A learning algorithm can modify its model (can
"learn") as additional training examples are given. A typical
application using Boxer would use only one Learner instance;
however, it is possible to have several instances in the same
application, e.g. in order to compare performance of different
Learners, or of the same algorithm with different
parameters.

</ul>

<h2>See also</h2>
<ul>
<li><a href="../../../../../boxer-user-guide.html">BOXER User Guide</a>
<li><a href="../../../../../standard-scenarios.html">Standard Discrimination Handling Scenarios</a> - more usage examples
<li><a href="../../../../../tags.html">Overview of the XML elements used by BOXER</a>
<li><a href="../../../../../nd.html">Treatment of new discrimination/class labels when parsing a data set</a>
</ul>

*/
package edu.dimacs.mms.boxer;

/*
Copyright 2009, Rutgers University, New Brunswick, NJ.

All Rights Reserved

Permission to use, copy, and modify this software and its documentation for any purpose 
other than its incorporation into a commercial product is hereby granted without fee, 
provided that the above copyright notice appears in all copies and that both that 
copyright notice and this permission notice appear in supporting documentation, and that 
the names of Rutgers University, DIMACS, and the authors not be used in advertising or 
publicity pertaining to distribution of the software without specific, written prior 
permission.

RUTGERS UNIVERSITY, DIMACS, AND THE AUTHORS DISCLAIM ALL WARRANTIES WITH REGARD TO 
THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR 
ANY PARTICULAR PURPOSE. IN NO EVENT SHALL RUTGERS UNIVERSITY, DIMACS, OR THE AUTHORS 
BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER 
RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, 
NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR 
PERFORMANCE OF THIS SOFTWARE.
*/
