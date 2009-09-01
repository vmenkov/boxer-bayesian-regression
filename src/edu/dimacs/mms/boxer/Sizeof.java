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