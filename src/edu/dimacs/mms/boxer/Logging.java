package edu.dimacs.mms.boxer;

import java.util.logging.*;

/** Methods used by Boxer classes to log warning and error
 * messages. These methdos are simply wrappers around the respective
 * methods of  java.util.logging.Logging.
 */
public class Logging {
    public final static String NAME = "boxer";

    public static void error(String msg) {
	Logger logger = Logger.getLogger(NAME);
	logger.severe(msg);
    }

    public static void warning(String msg) {
	Logger logger = Logger.getLogger(NAME);
	logger.warning(msg);
    }

    public static void info(String msg) {
	Logger logger = Logger.getLogger(NAME);
	logger.info(msg);
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