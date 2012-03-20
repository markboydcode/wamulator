package org.lds.sso.appwrap.ui.rest;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.lds.sso.appwrap.identity.coda.CodaServiceResponseParser;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CodaServiceResponseParserTest {

	private String loadTestContent(String file) throws IOException {
		ClassLoader cldr = CodaServiceResponseParserTest.class.getClassLoader();
		InputStream is = cldr.getResourceAsStream(file);
		DataInputStream dis = new DataInputStream(is);
		byte[] bytes = new byte[dis.available()];
		dis.read(bytes);
		return new String(bytes);
	}
	
	@Test
	public void testUserDataResponse() throws IOException {
		String content = loadTestContent("CodaServiceResponseParserTest-good.txt");
		CodaServiceResponseParser parser = new CodaServiceResponseParser(content);
		Map<String, String> m = parser.getValues();
		Assert.assertEquals(m.get("birthdate"), "1980-03-31");
		Assert.assertEquals(m.get("cn"), "pholder");
		Assert.assertEquals(m.get("gender"), "M");
		Assert.assertEquals(m.get("givenName"), "Perry");
		Assert.assertEquals(m.get("individualId"), "0083419004078");
		Assert.assertEquals(m.get("ldsAccountId"), "1");
		Assert.assertEquals(m.get("ldsMrn"), "0083419004078");
		Assert.assertEquals(m.get("positions"), "P57:W555005:S555001:A555000");
		Assert.assertEquals(m.get("preferredLanguage"), "en");
		Assert.assertEquals(m.get("preferredName"), "Perry Holder");
		Assert.assertEquals(m.get("sn"), "Holder");
		Assert.assertEquals(m.get("units"), "W555005:S555001:A555000");
	}
}
