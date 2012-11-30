package org.lds.sso.appwrap;

import org.testng.Assert;
import org.testng.annotations.Test;

public class UriMatcherTest {
	
	@Test
	public void exactMatchTest() {
		String matchUri = "/wamulator/test";
		
		String uri = "/wamulator/test";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/tester";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void universalMatchTest() {
		String matchUri = "/wamulator/test{/.../*,*}";
		
		String uri = "/wamulator/test/anything/else";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/test";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/stuff";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void universalMatchWithStarTest() {
		String matchUri = "/wamulat*r/test{/.../*,*}";
		
		String uri = "/wamulator/test/anything/else";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/test";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulatoor/test";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/stuff";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void universalMatchWithQuestionTest() {
		String matchUri = "/wamulat?r/test{/.../*,*}";
		
		String uri = "/wamulator/test/anything/else";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/test";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulatoor/stuff";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
		
		uri = "/wamulator/stuff";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void setTest() {
		String matchUri = "{/wamulator/scripts/*,/wamulator/style?/*}";
		
		String uri = "/wamulator/scripts/test.js";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/styles/test.css";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/scripts";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
		
		uri = "/wamulator/scripts/sub/test.css";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void embeddedSetTest() {
		String matchUri = "/wamulator/{scripts/*,styles/*}";
		
		String uri = "/wamulator/scripts/test.js";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/styles/test.css";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/scripts";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
		
		uri = "/wamulator/scripts/sub/test.css";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void embeddedSetVariationTest() {
		String matchUri = "/wamulator/{scripts,styles}/*";
		
		String uri = "/wamulator/scripts/test.js";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/styles/test.css";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/scripts";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
		
		uri = "/wamulator/scripts/sub/test.css";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void multipleSetTest() {
		String matchUri = "{/wamulator/,/emulator/}{scripts/*,styles/*}";
		
		String uri = "/wamulator/scripts/test.js";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/emulator/styles/test.css";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/scripts";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
		
		uri = "/emulator/scripts/sub/test.css";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void setUniversalMatchTest() {
		String matchUri = "{/wamulator,/emulator}{/.../*,*}";
		
		String uri = "/wamulator/test";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/emulator/test";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/something/test";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void starTest() {
		String matchUri = "*";
		
		String uri = "wamulator";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
		
		uri = "wamulator/blah";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void multipleStarTest() {
		String matchUri = "/wam*/*";
		
		String uri = "/wamulator/blah";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/blah/something";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
		
		uri = "/wamulator";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void endStarTest() {
		String matchUri = "/wamulator/*";
		
		String uri = "/wamulator/blah";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/blah/something";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void embeddedStarTest() {
		String matchUri = "/wamulator/*/something";
		
		String uri = "/wamulator/blah/something";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator//something";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/something";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
		
		uri = "/wamulator/blah/blah/something";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void embeddedComplexStarTest() {
		String matchUri = "/wamulator/t*t";
		
		String uri = "/wamulator/test";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/toast";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/tt";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/t";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
		
		uri = "/wamulator/t/st";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void embeddedComplexStarStartTest() {
		String matchUri = "/*mulator/test";
		
		String uri = "/wamulator/test";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/emulator/test";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/mulator/test";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "wa/mulator/test";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void embeddedComplexStarEndTest() {
		String matchUri = "/wam*/test";
		
		String uri = "/wamulator/test";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wam-ulator/test";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wam/test";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/sub/test";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void embeddedMultipleStarTest() {
		String matchUri = "/wamulator/*me*";
		
		String uri = "/wamulator/me";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/signme";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/signmein";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/signmeout";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/signin";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void questionStarTest() {
		String matchUri = "?*";
		
		String uri = "stuff";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/stuff";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
		
		uri = "wamulator/stuff";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void embeddedQuestionStarTest() {
		String matchUri = "/wamulator/?*/something";
		
		String uri = "/wamulator/blah/something";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wamulator/something";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
		
		uri = "/wamulator/blah/blah/something";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void embeddedComplexQuestionStarStartTest() {
		String matchUri = "/?*mulator/test";
		
		String uri = "/wamulator/test";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/emulator/test";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/mulator/test";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
		
		uri = "wa/mulator/test";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void embeddedComplexQuestionStarEndTest() {
		String matchUri = "/wam?*/test";
		
		String uri = "/wamulator/test";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wam-ulator/test";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "/wam/test";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
		
		uri = "/wamulator/sub/test";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}
	
	@Test
	public void queryStringTest() {
		String matchUri = "*signmein*";
		
		String uri = "something=another&signmein";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "something=another&signmeinn";
		Assert.assertTrue(UriMatcher.matches(uri, matchUri), uri + " SHOULD match " + matchUri);
		
		uri = "something=another&signmeout";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
		
		uri = "something=another&signmei";
		Assert.assertFalse(UriMatcher.matches(uri, matchUri), uri + " SHOULD NOT match " + matchUri);
	}

}
