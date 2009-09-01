package edu.dimacs.mms.boxer;

/** An exception of this type may be thrown by Boxer during parsing
 * XML. It is triggered when an input XML file, even though
 * well-formatted, does not have the structure expected by BOXER, or
 * contains values that we don't allow.
 */
public class BoxerXMLException extends BoxerException {
    BoxerXMLException(String msg) {
	super(msg);
    }
} 