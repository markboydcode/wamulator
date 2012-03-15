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
		Assert.assertEquals(m.get(CodaServiceResponseParser.BIRTH_DATE), "1980-03-31");
		Assert.assertEquals(m.get(CodaServiceResponseParser.CN), "pholder");
		Assert.assertEquals(m.get(CodaServiceResponseParser.GENDER), "M");
		Assert.assertEquals(m.get(CodaServiceResponseParser.GIVEN_NAME), "Perry");
		Assert.assertEquals(m.get(CodaServiceResponseParser.INDIVIDUAL_ID), "0083419004078");
		Assert.assertEquals(m.get(CodaServiceResponseParser.LDS_ACCOUNT_ID), "1");
		Assert.assertEquals(m.get(CodaServiceResponseParser.LDS_MRN), "0083419004078");
		Assert.assertEquals(m.get(CodaServiceResponseParser.POSITIONS), "P57:W555005:S555001:A555000");
		Assert.assertEquals(m.get(CodaServiceResponseParser.PREFERRED_LANG), "en");
		Assert.assertEquals(m.get(CodaServiceResponseParser.PREFERRED_NAME), "Perry Holder");
		Assert.assertEquals(m.get(CodaServiceResponseParser.SN), "Holder");
		Assert.assertEquals(m.get(CodaServiceResponseParser.UNITS), "W555005:S555001:A555000");
	}

	@Test
	public void testUserMissingDataResponse() throws IOException {
		String content = loadTestContent("CodaServiceResponseParserTest-bad.txt");
		CodaServiceResponseParser parser = new CodaServiceResponseParser(content);
		Map<String, String> m = parser.getValues();
		Assert.assertEquals(m.get("good"), "false");
		Assert.assertEquals(m.get("message"), "pholderhjkjk");
	}
}
