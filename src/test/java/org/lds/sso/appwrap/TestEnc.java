package org.lds.sso.appwrap;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.testng.annotations.Test;

public class TestEnc {
	String token = "AQIC5wM2LY4SfcwARlQjLPuGOZ7AKv/7vBVEfSZD8yWxAho=@AAJTSQACMDE=#";

	@Test
	public void testEnc() throws UnsupportedEncodingException {
		System.out.println(URLEncoder.encode(token, "utf-8"));
	}
	
	@Test
	public void testSplit() {
		String acts = "GET, POST".replace(" ", "");
		System.out.println(Arrays.asList(acts.split(",")).toString());
		String[] list = new String[] {"PP", "DD", "HH", "MM", "AA", "BB"};
		Set set = new TreeSet<String>(Arrays.asList(list));
		System.out.println(set);
	}
}
