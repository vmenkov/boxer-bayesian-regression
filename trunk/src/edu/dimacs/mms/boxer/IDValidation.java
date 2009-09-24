package edu.dimacs.mms.boxer;

/** Methods of this class are used for validating names of
 * discrimination, classes, data sets, data points, feature labels,
 * etc.
 */

public class IDValidation {

    boolean valid;
    String errmsg;
    boolean special;

    /*
    static class BoxerIDValidationException extends BoxerException {
	BoxerIDValidationException(String msg) {
	    super(msg);
	}	
    }
    */

    /** Checks that the specified string is a properly formatted "boxer ID"
	(same format for discrimination, class, data point, etc names)
	 
	@throws BoxerIDValidationException if it isn't
     */
    private IDValidation(String s) {
	valid = false;
	if (s.length()==0) {
	    errmsg="Empty string";
	    return;
	}
	if (s.indexOf(BXRReader.PAIR_SEPARATOR) >= 0) {
	    errmsg = "Separator character ("+BXRReader.PAIR_SEPARATOR+") found in string `"+s+"'";
	    return;
	}
	for(int i=0; i<s.length(); i++) {
	    if (Character.isSpaceChar( s.charAt(i))) {
		errmsg = "White space found in string `"+s+"'";
		return;
	    }
	}
	// special ID
	special = isSpecial(s);
	valid = true;
    }

    public static boolean isSpecial(String s) {
	return s.startsWith("@") && !s.startsWith("@@");
    }

    /** Takes a string and returns true if it syntactically is a legal
     * user-defined ID in BOXER.  It returns false if the string is a
     * legal BOXER-restricted ID (a "special ID"), or if it is a
     * string not legal as an ID.
     */
    public static boolean validateBasic(String s) {
	IDValidation val = new IDValidation(s);
	if (!val.valid) Logging.error(val.errmsg);
	return val.valid && !val.special;
    }

    /** there are no special names for discrimination IDs */
    static boolean validateDiscName(String s) {
	return  validateBasic(s);
    }

    static boolean validateFeatureName(String s) {
	IDValidation val = new IDValidation(s);
	if (!val.valid) Logging.error(val.errmsg);
	if (!val.valid) return false;
	if (!val.special) return true;
	return s.equals(FeatureDictionary.DUMMY_LABEL);
    }

    static boolean validateClaName(String s) {
	IDValidation val = new IDValidation(s);
	if (!val.valid) Logging.error(val.errmsg);
	if (!val.valid) return false;
	if (!val.special) return true;
	return s.equals(Suite.DIS_NAME) || s.equals(Suite.NOT_DIS_NAME) ||
	    Discrimination.isAPreReservedName(s);
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
