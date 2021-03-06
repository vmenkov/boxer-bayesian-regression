package edu.dimacs.mms.boxer.util;

import java.util.*;

/** An auxiliary class used in command line parsing by borj.Driver, learning.Repeater, and a few other BOXER applications. */
public class CMD {
    /** All command-line commands acceptable by the Driver */
    public final static String 
	TRAIN = "train", TEST = "test", 
	VALIDATE="validate", 	    
	READ_SUITE="read-suite", 
	READ_LABELS="read-labels", 
	READ_PRIORS = "read-priors",
	WRITE_SUITE="write-suite", 
	WRITE_PRIORS = "write-priors",
	READ="read", 
	READ_LEARNER="read-learner", 
	WRITE="write",
	DELETE_DISCR="delete-discr";
    /** The command, such as "read" */
    public String cmd;
    /** The command's argument (usually, a file name) */
    final public String f;
    /** The second (optional) argument that may appear after some commands */
    final public String f2;
    
    /** List of commands that can take an optional second
     * argument. All other commands only may take one argument.
     */
    static private String[] twoArgCmds = {TEST};
    static public void setTwoArgCmd(String x) {twoArgCmds = new String[] {x};}
    static public void setTwoArgCmd(String[] x) {twoArgCmds = x; }
    
    /** Similar to String.split(), but takes care not to split a
	single-quoted string. After processing, however, quotes
	themselves are stripped.
	
	This is added pursuant to Paul Raff's request,
	2009-06-18
    */
    private String[] mySplit(String s, char quote, char sep) {
	Vector<String> v = new Vector<String>(3);
	StringBuffer b = new StringBuffer(s.length()+1);
	
	boolean withinQuotes = false;
	for( int i=0; i<s.length(); i++ ) {
	    char c = s.charAt(i);
	    if (withinQuotes) {
		if (c== quote) {
		    withinQuotes = false;
		} else {
		    b.append(c);
		}
	    } else if (c==quote) {
		withinQuotes = true;
	    } else if (c==sep) {
		v.add(b.toString()); 
		b.setLength(0);
	    }  else {
		b.append(c);
	    }
	}
	v.add(b.toString());
	return v.toArray(new String[0]);
    }

    private static void usage(String m) {
	System.out.println("Poorly formatted command line:");
    	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }


    /** Creates a CMD object from a string that looks like
	"cmd:arg" or "cmd:arg1:arg2"
    */
    CMD(String s) {
	final boolean respectQuotes = true;
	String q[] = respectQuotes? mySplit(s, '\'', ':') : s.split(":");
	if (q.length<2 || q[0].length()==0 || q[1].length()==0 ) {
	    usage("Command line contains an argument that can't be parsed as cmd:file or cmd:file1:filed2. Invalid argument=" + s);
	}
	cmd =q[0];
	f = q[1];
	f2 = (q.length >= 3) ? q[2] : null;
	// verifying that no unused args are left 
	if (q.length > maxArgAllowed()+1) {
	    usage("No command other than {"+join(",", twoArgCmds)+"} may have two arguments");
	} 
    }	

    public boolean is(String val) {
	return cmd.equals(val);
    }

    private int maxArgAllowed() {
	for(String q:  twoArgCmds) { if (cmd.equals(q)) return 2; }
	return 1;
    }


    public String toString() {
	return "(cmd=" + cmd + ", arg=" + f+")";
    }
    
    /** Parses an entire command line into an array of CMD objects */
    public static CMD[] parse(String [] argv) {
	int h=0;
	Vector<CMD> v = new  Vector<CMD>();
	while(h < argv.length) {
	    String s = argv[h++];
	    while (h < argv.length && 
		   (s.endsWith(":") || argv[h].startsWith(":"))) {
		s += argv[h++];
	    }
	    // a rare case of leading/trailing spaces in args - may appear when
	    // backslashes are used liberally
	    s = s.trim();
	    if (s.length()==0) continue;
	    v.addElement( new CMD(s));
	}
	return v.toArray(new CMD[0]);
    }	

    static private String join(String sep, String val[]) {
	StringBuffer b = new StringBuffer();
	for(String q: val) {
	    if (b.length()>0) b.append(sep);
	    b.append(q);
	}
	return b.toString();
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