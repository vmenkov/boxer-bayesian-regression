package edu.dimacs.mms.boxer;

import java.util.logging.*;

class Logging {
    final static String NAME = "boxer";

    static void warning(String msg) {
	Logger logger = Logger.getLogger(NAME);
	logger.warning(msg);
    }

    static void info(String msg) {
	Logger logger = Logger.getLogger(NAME);
	logger.info(msg);
    }


}