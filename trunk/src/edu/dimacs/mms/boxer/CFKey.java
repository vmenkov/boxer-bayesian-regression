package edu.dimacs.mms.boxer;

import java.util.*;

/** An auxiliary class for {@link Priors}: a compound key for hash
    maps; includes the class ID and feature ID.  Objects with the
    value of -1 in either the class ID or feature ID value are used to
    store priors that apply to all classes or all features.
*/
class CFKey implements Comparable<CFKey> {
	final static int ALL = -1;
	/** Stored in the class field when this is an L3 prior, and
	 * the match is by class name, not id
	 */
	final static int BY_NAME = -2;
	private int cid, fid;
	/** Only used for L3 fields */
	private String cname;

	public String toString() {
	    return "(fid="+fid+", cid="+cid+", cname="+cname+")";
	}

	int getFid() { 
	    return fid; 
	}
	/** Returns the class name for the class that's encoded either
	  by cid or (in the cross-discr case) by cname.

	    @param dis This must be the discrimination for which this key has been created (or null for a cross-discr data key)
	 */
	String getCName(Discrimination dis) {
	    if (cid==BY_NAME) return cname;
	    else if (cid == ALL) throw new IllegalArgumentException("Can't call CFKey.getCName() on "+this+", because this is not a class-specific prior key");
	    else if (dis == null) throw new IllegalArgumentException("getCName(dis=null) should not be called on a discrimination-specific prior");
	    else return dis.getClaById(cid).getName();
	}

	/** First by fid, then by cid */
	public int compareTo(CFKey x) {
	    int z = fid - x.fid;
	    return (z!=0) ? z : cid - x.cid;
	}

	public int  hashCode() {
	    return (cid << 16) | (fid & 0xFFFF);
	}
	CFKey(int c, int f) {
	    cid = c;
	    fid = f;
	}
	/** Creates a CFKey entry for (c, f) 
	    @param c If null is given, it means "ALL" */
	CFKey(Discrimination.Cla c, int f) {
	    this( c==null? ALL : c.getPos(), f); 
	}
	/** Creates a CFKey entry for (c, f=ALL) */
	CFKey(Discrimination.Cla c) {
	    this( c.getPos(), ALL); 
	}
	/** For L3 only */
	CFKey(String _cname) {
	    this( BY_NAME, ALL); 
	    cname = _cname;
	    if (!cname.equals(Suite.NOT_DIS_NAME)) {
		throw new IllegalArgumentException("The class name " + cname + " is not allowed for L3 priors. The only allowed name is " + Suite.NOT_DIS_NAME);
	    }
	}

	/** Is this an "all classes" key? */
	boolean isAllClasses() { return cid==ALL; }
	/** Is this an "all features" key? */
	boolean isAllFeatures() { return fid==ALL; }
}

