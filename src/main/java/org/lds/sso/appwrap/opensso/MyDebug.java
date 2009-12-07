/**
 * 
 */
package org.lds.sso.appwrap.opensso;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.debug.IDebug;

/**
 * Implementation of opensso's IDebug interface that wraps a Log4j Logger.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class MyDebug implements IDebug {
	private Logger cLog = null;
	private int level;
	private String name;
	
	/**
	 * Instantiates an instance wrapping a Log4j Logger with name equal to the
	 * the passed in String.
	 * 
	 * @param name
	 */
	public MyDebug(String name) {
		cLog = Logger.getLogger(name);
		this.name = name;
	}
	
	public void error(String msg, Throwable t) {
		cLog.error(msg, t);
	}

	public void error(String msg) {
		cLog.error(msg);
	}

	public void message(String msg, Throwable t) {
		cLog.debug(msg, t);
	}

	public void message(String msg) {
		cLog.debug(msg);
	}

	public boolean messageEnabled() {
		return cLog.isDebugEnabled();
	}

	public boolean errorEnabled() {
		return true; // always true for log4j, turned off via config file
	}

	public String getName() {
		return name;
	}

	public int getState() {
		return level; 
	}

	public void resetDebug(String mf) {
	}

	public void setDebug(int level) {
		this.level = level;
	}

	public void setDebug(String level) {
		if (Debug.STR_ERROR.equals(level)) {
			cLog.setLevel(Level.ERROR);
			setDebug(Debug.ERROR);
		}
		else if (Debug.STR_MESSAGE.equals(level)) {
			cLog.setLevel(Level.DEBUG);
			setDebug(Debug.MESSAGE);
		}
		else if (Debug.STR_OFF.equals(level)) {
			cLog.setLevel(Level.OFF);
			setDebug(Debug.OFF);
		}
		else if (Debug.STR_WARNING.equals(level)) {
			cLog.setLevel(Level.WARN);
			setDebug(Debug.WARNING);
		}
		else if (Debug.STR_ON.equals(level)) {
			cLog.setLevel(Level.DEBUG);
			setDebug(Debug.ON);
		}
		else {
			cLog.setLevel(Level.OFF);
			setDebug(Debug.OFF);
		}
	}

	public void warning(String msg, Throwable t) {
		cLog.warn(msg, t);
	}

	public boolean warningEnabled() {
		return true; // warn is always on for log4j
	}
}