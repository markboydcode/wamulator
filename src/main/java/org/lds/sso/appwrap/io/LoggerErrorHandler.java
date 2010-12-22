package org.lds.sso.appwrap.io;

import java.util.logging.Logger;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class LoggerErrorHandler extends DefaultHandler {
	private Logger logger;
	
	public LoggerErrorHandler(Logger logger) {
		this.logger = logger;
	}
	
    public void warning(SAXParseException e) throws SAXException { 
        printInfo(e);
     }
     public void error(SAXParseException e) throws SAXException { 
        printInfo(e);
     }
     public void fatalError(SAXParseException e) throws SAXException { 
        printInfo(e);
     }
     private void printInfo(SAXParseException e) {
     	 LogUtils.warning(logger, "{0}, Line: {1}, Column: {2}", e.getMessage(), e.getLineNumber(), e.getColumnNumber());
     }
}
