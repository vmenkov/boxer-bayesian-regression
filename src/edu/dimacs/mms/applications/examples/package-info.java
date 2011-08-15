/**    A few very simple applications to demonstrate data input (in
 *    various formats) and model training.

       Each application in this package uses the BOXER API to train a
       PLRM on labeled data.  All of the applications named
       SimpleTrain_* produce the same trained model from the same
       training data, but show off different ways of getting training
       data into BOXER, and getting the trained PLRMs out of BOXER.

       In each version we create the Suite containing the trained
       PLRM in roughly the same form the training data was presented
       in (e.g. a file, a string, etc.).  We then convert that form
       to printed XML and output it.  The XML output for the
       different applications can be compared to see that the same
       result is produced in all cases (with one exception noted in
       SimpleTrain_SVMlightfiles.java).

       In real applications one could of course mix any of the API
       styles shown in the SimpleTrain_* applications, for instance
       supplying the training data as an in-memory string, but
       writing the trained PLRM directly to a file.

       We do not start with a Suite definition or Prior definition in
       any of these cases. Almost all aspects of learning occur as
       specified by the BOXER defaults, and the nature of the
       discrimination is inferred from the labels on the training
       data.  The one exception is that do use method ?????? to force
       class ??? to be a reference class.  This allows us to force
       all the trained models to have nonzero coefficients only for
       class ???, including the model produced by
       SimpleTrain_BBRfiles which uses a format that requires a
       reference class.
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
