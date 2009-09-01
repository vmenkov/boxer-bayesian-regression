package boxer;

/** An exception of this class, or subclass, may be thrown by Boxer on
 * encountering certain problems */
public class BoxerException extends Exception {
    BoxerException(String msg) {
	super(msg);
    }
} 