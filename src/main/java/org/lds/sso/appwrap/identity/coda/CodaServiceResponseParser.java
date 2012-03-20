package org.lds.sso.appwrap.identity.coda;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Parser of content response of coda user attributes service located at the
 * URL specified in the configuration file's &lt;users&gt; element in its
 * source attribute. The structure of the response when requested with an
 * accept header of "application/json", as of this writting, is:
 * 
 * <pre>
 *	&lt;org.lds.community.data.ws.dto.OssoMemberDto&gt;
 *	  &lt;birthdate&gt;1980-03-31&lt;/birthdate&gt;
 *	  &lt;cn&gt;pholder&lt;/cn&gt;
 *	  &lt;gender&gt;M&lt;/gender&gt;
 *	  &lt;givenName&gt;Perry&lt;/givenName&gt;
 *	  &lt;individualId&gt;0083419004078&lt;/individualId&gt;
 *	  &lt;ldsAccountId&gt;1&lt;/ldsAccountId&gt;
 *	  &lt;ldsMrn&gt;0083419004078&lt;/ldsMrn&gt;
 *	  &lt;positions&gt;P57:W555005:S555001:A555000&lt;/positions&gt;
 *	  &lt;preferredLanguage&gt;en&lt;/preferredLanguage&gt;
 *	  &lt;preferredName&gt;Perry Holder&lt;/preferredName&gt;
 *	  &lt;sn&gt;Holder&lt;/sn&gt;
 *	  &lt;status&gt;200&lt;/status&gt;
 *	  &lt;units&gt;W555005:S555001:A555000&lt;/units&gt;
 *	&lt;/org.lds.community.data.ws.dto.OssoMemberDto&gt;
 * </pre>
 * 
 * If the information for the specified user is not found then the response is:
 * 
 *  <pre>
 *  
<org.lds.community.data.ws.dto.ResponseStatus>
  <good>false</good>
  <message>pholderhjkjk</message>
</org.lds.community.data.ws.dto.ResponseStatus>
 * 
 * This parser drops the root element and for each nested element maps the 
 * element name to an SSO header name as the key injected into the values map
 * with the textual content of that element if not empty. If there the element
 * name does not match any SSO header name then it is injected as is ostens
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2010, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class CodaServiceResponseParser implements ContentHandler{

	/**
	 * Accumulates character content within an element.
	 */
	StringWriter chars = new StringWriter();
	
	/**
	 * Keeps track of how deeply we are within the element tree.
	 */
	int depth = 0;
	
	/**
	 * Keeps track of the element name which may end up being matched to sso
	 * user attributes and thereby header values injected for the user.
	 */
	private String attName;
	
	/**
	 * The resulting map loaded from parsing the content.
	 */
	private Map<String,String> attValues = new HashMap<String,String>();
	
	/**
	 * The content that is/was parsed.
	 */
	private String contentBeingParsed;

	/**
	 * Constructs a new parser and parses the coda response content loading it
	 * into a map available from {@link #getValues()}.
	 * 
	 * @param content
	 */
	public CodaServiceResponseParser(String content) {
		contentBeingParsed = content;
		parse();
	}
	
	/**
	 * Returns the content that was parsed.
	 * @return
	 */
	public String getContent() {
		return contentBeingParsed;
	}
	
	/**
	 * Parses the content extracting the keys and values found therein
	 * and placing them into a map with keys translated to SSO header names
	 * if the key matches one defined in {@link #coda2sso}.
	 * 
	 * @return
	 */
	private final void parse() {
		XMLReader rdr;
		try {
			rdr = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
		}
		catch (Exception e) {
			throw new RuntimeException("Unable to create parser for coda response of '" 
					+ this.contentBeingParsed
					+ "'.", e);
		}
		rdr.setContentHandler(this);
		InputSource src = new InputSource(new StringReader(contentBeingParsed));
		try {
			rdr.parse(src);
		}
		catch (Exception e) {
			throw new RuntimeException("Unable to parse coda response of '" 
					+ this.contentBeingParsed 
					+ "'.", e);
		}
	}

	/**
	 * Returns the Map of keys and values found in the coda response content 
	 * with keys translated to SSO header names if the key matched those in 
	 * {@link #coda2sso}.
	 * 
	 * @return
	 */
	public Map<String, String> getValues() {
		return this.attValues;
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		chars.write(ch, start, length);
	}

	public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {
		depth++;
		if (depth == 2) {
			attName = name;
		}
		chars.getBuffer().setLength(0);
	}

	public void endElement(String uri, String localName, String name) throws SAXException {
		if (depth == 2) {
			attValues.put(name, chars.toString());
		}
		depth--;
	}

	public void endPrefixMapping(String prefix) throws SAXException {
	}

	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
	}

	public void processingInstruction(String target, String data) throws SAXException {
	}

	public void setDocumentLocator(Locator locator) {
	}

	public void skippedEntity(String name) throws SAXException {
	}

	public void startDocument() throws SAXException {
	}

	public void startPrefixMapping(String prefix, String uri) throws SAXException {
	}

	public void endDocument() throws SAXException {
	}
}
