package org.lds.sso.appwrap.ui.rest;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
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
		Assert.assertEquals(m.get(UserHeaderNames.BIRTH_DATE), "1980-03-31");
		Assert.assertEquals(m.get(UserHeaderNames.PREFIX + "cn"), "pholder");
		Assert.assertEquals(m.get(UserHeaderNames.GENDER), "M");
		Assert.assertEquals(m.get(UserHeaderNames.PREFIX + "given-name"), "Perry");
		Assert.assertEquals(m.get(UserHeaderNames.INDIVIDUAL_ID), "0083419004078");
		Assert.assertEquals(m.get(UserHeaderNames.LDS_ACCOUNT_ID), "1");
		Assert.assertEquals(m.get(UserHeaderNames.LDS_MRN), "0083419004078");
		Assert.assertEquals(m.get(UserHeaderNames.POSITIONS), "P57:W555005:S555001:A555000");
		Assert.assertEquals(m.get(UserHeaderNames.PREFIX + "preferred-language"), "en");
		Assert.assertEquals(m.get(UserHeaderNames.PREFIX + "preferred-name"), "Perry Holder");
		Assert.assertEquals(m.get(UserHeaderNames.PREFIX + "sn"), "Holder");
		Assert.assertEquals(m.get(UserHeaderNames.UNITS), "W555005:S555001:A555000");
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
