package edu.dimacs.mms.boxer;

import java.util.*;

/** Constants and utilities for estimating memory use of some data structures */
public class Sizeof {
    /** Sizes, in bytes, of various things */
    static final int OBJ=8, OBJREF=4, DOUBLE=8, INT=4;
    
    public static long sizeof(int v[]) {
	return  (v==null) ? 0 : OBJ + v.length * INT;
    }

    public static long sizeof(double v[]) {
	return  (v==null) ? 0 : OBJ + v.length * DOUBLE;
    }

    public static long sizeof(Vector<? extends Measurable> v) {
	if (v==null) return 0;
	long sum = OBJ;
	for(Measurable p: v) sum += p.memoryEstimate();
	return sum;
    }

    public static long sizeof(double w[][]) {
	if (w==null) return 0;
	long sum = OBJ + w.length * OBJREF;
	for(double v[]: w) sum +=  sizeof(v);
	return sum;
    }
	

}

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