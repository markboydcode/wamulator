package org.lds.sso.appwrap.io;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class LogUtils {
	private LogUtils() {
		// thwart instantiation
	}

	public static void info(Logger logger, String msg, Object... args) {
		log(logger, Level.INFO, msg, null, args);
	}
	
	public static void fine(Logger logger, String msg, Object... args) {
		log(logger, Level.FINE, msg, null, args);
	}
	
	public static void warning(Logger logger, String msg, Object... args) {
		log(logger, Level.WARNING, msg, null, args);
	}

	public static void severe(Logger logger, String msg, Object... params) {
		log(logger, Level.SEVERE, msg, null, params);
	}
	
	/**
     * Log a SEVERE message, with a throwable, and with optional Object parameters, 
     * using the specified logger.
     * 
     * @param logger The Logger to use in logging the message
     * @param msg The string message to be logged
     * @param params Vararg of Object parameters to be replaced in the message if the message is logged
     */
	public static void severe(Logger logger, String msg, Throwable throwable, Object... params) {
		log(logger, Level.SEVERE, msg, throwable, params);
	}

	/**
     * Log throwing an exception with optional Object parameters, using the specified logger.  
     * This will only be logged if the logger level is FINE or greater.
     * 
     * @param logger The Logger to use in logging the exception
     * @param msg The string message to be logged
     * @param throwable Throwable to be logged
     * @param params Vararg of Object parameters to be replaced in the message if the message is logged
     */
	public static void throwing(Logger logger, String msg, Throwable throwable, Object... params) {
		log(logger, Level.FINE, msg, throwable, params);
	}
	
	/**
	 * Borrowed from Java Stack Log Utils module
	 * 
	 * Log a message, at the specified level, with a Throwable, and with 
     * optional Object parameters, using the specified logger.
     * 
     * <p>
     * If the logger is currently enabled for the given message 
     * level then a corresponding LogRecord is created and forwarded 
     * to all the registered output Handler objects.
     * <p>
     * 
     * @param logger The Logger to use in logging the message
     * @param level One of the message level identifiers, e.g. SEVERE
     * @param msg	The string message to be logged
     * @param throwable Throwable to be logged
     * @param params	Vararg of Object parameters to be replaced in the 
     * message if the message is logged
     * 
     * @see java.util.Logger.log(Level level, String msg, Object params[]);
     * @see org.lds.stack.logging.LogUtils.log(Logger logger, Level level, String msg, Throwable throwable, Object... params); 
	 */
	public static void log(Logger logger, Level level, String msg, Throwable throwable, Object... params) {
		if(!logger.isLoggable(level)) {
			return;
		}
		LogRecord logRecord = new LogRecord(level, msg);
		logRecord.setParameters(params);
		logRecord.setThrown(throwable);
		CallerInfo callerInfo = inferCaller();
		logRecord.setLoggerName(logger.getName());
		logRecord.setSourceClassName(callerInfo.getSourceClassName());
		logRecord.setSourceMethodName(callerInfo.getSourceMethodName());
		logger.log(logRecord);
	}
	
	/**
	 * Borrowed from Java Stack Log Utils module
	 * 
	 * @see org.lds.stack.logging.LogUtils.inferCaller();
	 */
    private static CallerInfo inferCaller() {
    	// Get the stack trace.
    	StackTraceElement stack[] = (new Throwable()).getStackTrace();
    	// First, search back to a method in the Logger class.
    	int index = 0;
    	while (index < stack.length) {
    	    StackTraceElement frame = stack[index];
    	    String cname = frame.getClassName();
    	    if (cname.contains(LogUtils.class.getName())) {
    	    	break;
    	    }
    	    index++;
    	}
    	// Now search for the first frame before the "Logger" class.
    	while (index < stack.length) {
    	    StackTraceElement frame = stack[index];
    	    String cname = frame.getClassName();
    	    if (!cname.equals(LogUtils.class.getName())) {
    	    	// We've found the relevant frame.
    	    	return new CallerInfo(cname, frame.getMethodName());
    	    }
    	    index++;
    	}
    	// We haven't found a suitable frame, so just punt.  This is
        // OK as we are only committed to making a "best effort" here.
    	return new CallerInfo();
    }
    
    /**
	 * Borrowed from Java Stack Log Utils module
	 * 
	 * @see org.lds.stack.logging.LogUtils.CallerInfo;
	 */
    private static class CallerInfo {
    	private String sourceClassName;
    	private String sourceMethodName;
    	
    	public CallerInfo() {}
    	public CallerInfo(String sourceClassName, String sourceMethodName) {
    		this.sourceClassName = sourceClassName;
    		this.sourceMethodName = sourceMethodName;
    	}
    	
    	public String getSourceClassName() {
    		return sourceClassName;
    	}
    	public String getSourceMethodName() {
    		return sourceMethodName;
    	}
    }



}
