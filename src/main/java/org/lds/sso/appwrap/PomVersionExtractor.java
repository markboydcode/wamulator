package org.lds.sso.appwrap;

import java.io.ByteArrayOutputStream; 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;

import org.lds.sso.appwrap.XmlConfigLoader2.Path;
import org.lds.sso.appwrap.rest.RestVersion;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Class used only when running in an IDE where maven filtering hasn't modified
 * the about.txt file exposing at runtime the version of the simulator.
 * 
 * @author BoydMR
 *
 */
public class PomVersionExtractor {

	public static String getVersion(Reader reader, String sourceInfo) throws Exception {
		XMLReader rdr;
		try {
			rdr = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
		}
		catch (Exception e) {
			throw new Exception("Unable to create parser for extracting version from pom at '" 
					+ sourceInfo + "'.", e);
		}
		PomContentHandler hndlr = new PomContentHandler(); 
		rdr.setContentHandler(hndlr);
		InputSource src = new InputSource(reader);
		try {
			rdr.parse(src);
			return hndlr.version;
		}
		catch (Exception e) {
			throw new Exception("Unable to parse pom '" 
					+ sourceInfo + "'.", e);
		}
	}
	
	public static class PomContentHandler implements ContentHandler {
		
		StringBuffer characters = null;
		protected Path path = new Path();
		String version = null;

		public void processingInstruction(String target, String data) throws SAXException {
		}

		public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {
			characters = new StringBuffer();
			path.add(name);
		}

		public void endElement(String uri, String localName, String name) throws SAXException {
		    if (path.matches("/project/version")) {
		        version = characters.toString();
		    }
			path.remove(name);
		}

		public void characters(char[] ch, int start, int length) throws SAXException {
			characters.append(ch, start, length);
		}

		public void endDocument() throws SAXException {
		}

		public void endPrefixMapping(String prefix) throws SAXException {
		}

		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		}

		public void setDocumentLocator(Locator locator) {
		}

		public void skippedEntity(String name) throws SAXException {
		}

		public void startDocument() throws SAXException {
		}

		public void startPrefixMapping(String prefix, String uri) throws SAXException {
		}
	}
}
