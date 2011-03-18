package edu.dimacs.mms.applications.util;

//import edu.dimacs.mms.boxer.ParseConfig;

/** An instance of this class stores various flags controlling the
    processing of input data source files. */
public class TokenizerOptions {

    /** This flag indicates if we use tokens (words, mostly) as features. 
	We normally do, of course, unless the user explicitly asks not to
	use them (so that he can only use n-grams instead)
    */
    public boolean useWords = true;

    /** Max length of an "n-gram". E.g. if it's 0, we don't index on
	characters; if 1, we use single chars as features; if 2, we also 
	use 2-grams, etc.
     */
    public int  maxCharSeqLen = 2;

    /** If true, simply skip empty cells, creating no feature vectors
	for them */
    public boolean emptySkip;
    /** If true, use a special feature, "@@empty", for empty cells (instead 
	of simply a zero vector.)
     */ 
    public boolean emptySpecial; 
}
