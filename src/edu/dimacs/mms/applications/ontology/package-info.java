/**  The Bayesian Ontology Aligner (BOA). This is a tool that allows
   to match ("align") the fields of one ontology (the scheme of one
   data source) with those of another ontology, based on the content
   of these fields in the data source records.

<h2>Algorithms</h2>
For an overview of the algorithms available for ontology matching, please
see the following PDF document: <a href="../../../../../../../pdf/boa-01.pdf">On choosing a suitable score function for the Bayesian Ontology Alignment tool</a>.

<h2>Technical details</h2>

<p>For the information on using this package, including the
command-line format, please read about its main class, {@link
edu.dimacs.mms.applications.ontology.Driver}

<p> This package uses both BOXER and (to a minor extent) BORJ. the
latter is used for such things as command-line parsing.

*/
package edu.dimacs.mms.applications.ontology;

/*
Copyright 2010, Rutgers University, New Brunswick, NJ.

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
