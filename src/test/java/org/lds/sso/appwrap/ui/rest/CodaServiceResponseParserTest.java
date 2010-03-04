package org.lds.sso.appwrap.ui.rest;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.lds.sso.plugins.authz.LegacyPropsInjector;
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
		Assert.assertEquals(m.get(LegacyPropsInjector.CP_BIRTH_DATE), "1980-03-31");
		Assert.assertEquals(m.get(LegacyPropsInjector.CP_PREFIX + "cn"), "pholder");
		Assert.assertEquals(m.get(LegacyPropsInjector.CP_GENDER), "M");
		Assert.assertEquals(m.get(LegacyPropsInjector.CP_PREFIX + "given-name"), "Perry");
		Assert.assertEquals(m.get(LegacyPropsInjector.CP_INDIVIDUAL_ID), "0083419004078");
		Assert.assertEquals(m.get(LegacyPropsInjector.CP_LDS_ACCOUNT_ID_PROPERTY), "1");
		Assert.assertEquals(m.get(LegacyPropsInjector.CP_LDS_MRN), "0083419004078");
		Assert.assertEquals(m.get(LegacyPropsInjector.CP_POSITIONS_SESSION_PROPERTY), "P57:W555005:S555001:A555000");
		Assert.assertEquals(m.get(LegacyPropsInjector.CP_PREFIX + "preferred-language"), "en");
		Assert.assertEquals(m.get(LegacyPropsInjector.CP_PREFIX + "preferred-name"), "Perry Holder");
		Assert.assertEquals(m.get(LegacyPropsInjector.CP_PREFIX + "sn"), "Holder");
		Assert.assertEquals(m.get(LegacyPropsInjector.CP_UNITS_SESSION_PROPERTY), "W555005:S555001:A555000");
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
