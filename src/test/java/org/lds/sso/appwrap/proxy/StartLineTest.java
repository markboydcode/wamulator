package org.lds.sso.appwrap.proxy;

import java.net.MalformedURLException;

import org.testng.Assert;
import org.testng.annotations.Test;

public class StartLineTest {

    @Test
    public void testBadRequestLine() throws MalformedURLException {
        StartLine sl = new StartLine("GET http://some.host/some/url"); // missing http decl

        Assert.assertEquals(sl.isAbsReqURI(), false);
        Assert.assertEquals(sl.isBadLine(), true); // the real indicator
        Assert.assertEquals(sl.isRequestLine(), false);
        Assert.assertEquals(sl.isResponseLine(), false);

        RequestLine rl = sl;
        Assert.assertEquals(rl.getAbsReqUri_host(), null);
        Assert.assertEquals(rl.getAbsReqUri_port(), -1);
        Assert.assertEquals(rl.getAbsReqUri_scheme(), null);
        Assert.assertEquals(rl.getAbsReqUriUsesDefaultPort(), false);
        Assert.assertEquals(rl.getHttpDecl(), "GET http://some.host/some/url"); // returns whole line since can't parse
        Assert.assertEquals(rl.getHttpVer(), null);
        Assert.assertEquals(rl.getMethod(), "GET http://some.host/some/url"); // returns whole line since can't parse
        Assert.assertEquals(rl.getReqFragment(), null);
        Assert.assertEquals(rl.getReqPath(), null);
        Assert.assertEquals(rl.getReqQuery(), null);
        Assert.assertEquals(rl.getUri(), "GET http://some.host/some/url"); // returns whole line since can't parse

        ResponseLine rpl = sl;
        Assert.assertEquals(rpl.getHttpDecl(), "GET http://some.host/some/url"); // returns whole line since can't parse
        Assert.assertEquals(rpl.getHttpVer(), null);
        Assert.assertEquals(rpl.getMsg(), "GET http://some.host/some/url"); // returns whole line since can't parse
        Assert.assertEquals(rpl.getRespCode(), "GET http://some.host/some/url"); // returns whole line since can't parse
    }

    @Test
    public void testAbsRequestLine() throws MalformedURLException {
        StartLine sl = new StartLine("GET http://some.host:27/some/url?box=1&sam=2|3&jane=4#fragment HTTP/1.1");

        Assert.assertEquals(sl.isAbsReqURI(), true);
        Assert.assertEquals(sl.isBadLine(), false);
        Assert.assertEquals(sl.isRequestLine(), true);
        Assert.assertEquals(sl.isResponseLine(), false);

        RequestLine rl = sl;
        Assert.assertEquals(rl.getAbsReqUri_host(), "some.host");
        Assert.assertEquals(rl.getAbsReqUri_port(), 27);
        Assert.assertEquals(rl.getAbsReqUri_scheme(), "http");
        Assert.assertEquals(rl.getAbsReqUriUsesDefaultPort(), false);
        Assert.assertEquals(rl.getHttpDecl(), "HTTP/1.1");
        Assert.assertEquals(rl.getHttpVer(), "1.1");
        Assert.assertEquals(rl.getMethod(), "GET");
        Assert.assertEquals(rl.getReqFragment(), "fragment");
        Assert.assertEquals(rl.getReqPath(), "/some/url");
        Assert.assertEquals(rl.getReqQuery(), "box=1&sam=2|3&jane=4");
        Assert.assertEquals(rl.getUri(), "/some/url?box=1&sam=2|3&jane=4#fragment");
    }

    @Test
    public void test_String_AbsDefPort443RequestLine() throws MalformedURLException {
        StartLine sl = new StartLine("GET https://some.host/some/url?box=1&sam=2|3&jane=4#fragment HTTP/1.1");

        Assert.assertEquals(sl.isAbsReqURI(), true);
        Assert.assertEquals(sl.isBadLine(), false);
        Assert.assertEquals(sl.isRequestLine(), true);
        Assert.assertEquals(sl.isResponseLine(), false);

        RequestLine rl = sl;
        Assert.assertEquals(rl.getAbsReqUri_host(), "some.host");
        Assert.assertEquals(rl.getAbsReqUri_port(), 443);
        Assert.assertEquals(rl.getAbsReqUri_scheme(), "https");
        Assert.assertEquals(rl.getAbsReqUriUsesDefaultPort(), true);
        Assert.assertEquals(rl.getHttpDecl(), "HTTP/1.1");
        Assert.assertEquals(rl.getHttpVer(), "1.1");
        Assert.assertEquals(rl.getMethod(), "GET");
        Assert.assertEquals(rl.getReqFragment(), "fragment");
        Assert.assertEquals(rl.getReqPath(), "/some/url");
        Assert.assertEquals(rl.getReqQuery(), "box=1&sam=2|3&jane=4");
        Assert.assertEquals(rl.getUri(), "/some/url?box=1&sam=2|3&jane=4#fragment");
    }


    @Test
    public void test_tokens_AbsDefPort443RequestLine() throws MalformedURLException {
        StartLine sl = new StartLine("GET", "https://some.host/some/url?box=1&sam=2|3&jane=4#fragment", "HTTP/1.1");

        Assert.assertEquals(sl.isAbsReqURI(), true);
        Assert.assertEquals(sl.isBadLine(), false);
        Assert.assertEquals(sl.isRequestLine(), true);
        Assert.assertEquals(sl.isResponseLine(), false);

        RequestLine rl = sl;
        Assert.assertEquals(rl.getAbsReqUri_host(), "some.host");
        Assert.assertEquals(rl.getAbsReqUri_port(), 443);
        Assert.assertEquals(rl.getAbsReqUri_scheme(), "https");
        Assert.assertEquals(rl.getAbsReqUriUsesDefaultPort(), true);
        Assert.assertEquals(rl.getHttpDecl(), "HTTP/1.1");
        Assert.assertEquals(rl.getHttpVer(), "1.1");
        Assert.assertEquals(rl.getMethod(), "GET");
        Assert.assertEquals(rl.getReqFragment(), "fragment");
        Assert.assertEquals(rl.getReqPath(), "/some/url");
        Assert.assertEquals(rl.getReqQuery(), "box=1&sam=2|3&jane=4");
        Assert.assertEquals(rl.getUri(), "/some/url?box=1&sam=2|3&jane=4#fragment");
    }

    @Test
    public void testAbsDefPort80RequestLine() throws MalformedURLException {
        StartLine sl = new StartLine("GET http://some.host/some/url?box=1&sam=2|3&jane=4#fragment HTTP/1.1");

        Assert.assertEquals(sl.isAbsReqURI(), true);
        Assert.assertEquals(sl.isBadLine(), false);
        Assert.assertEquals(sl.isRequestLine(), true);
        Assert.assertEquals(sl.isResponseLine(), false);

        RequestLine rl = sl;
        Assert.assertEquals(rl.getAbsReqUri_host(), "some.host");
        Assert.assertEquals(rl.getAbsReqUri_port(), 80);
        Assert.assertEquals(rl.getAbsReqUri_scheme(), "http");
        Assert.assertEquals(rl.getAbsReqUriUsesDefaultPort(), true);
        Assert.assertEquals(rl.getHttpDecl(), "HTTP/1.1");
        Assert.assertEquals(rl.getHttpVer(), "1.1");
        Assert.assertEquals(rl.getMethod(), "GET");
        Assert.assertEquals(rl.getReqFragment(), "fragment");
        Assert.assertEquals(rl.getReqPath(), "/some/url");
        Assert.assertEquals(rl.getReqQuery(), "box=1&sam=2|3&jane=4");
        Assert.assertEquals(rl.getUri(), "/some/url?box=1&sam=2|3&jane=4#fragment");
    }

    @Test
    public void test_String_RequestLine() throws MalformedURLException {
        StartLine sl = new StartLine("GET /some/url?box=1&sam=2|3&jane=4#fragment HTTP/1.1");

        Assert.assertEquals(sl.isAbsReqURI(), false);
        Assert.assertEquals(sl.isBadLine(), false);
        Assert.assertEquals(sl.isRequestLine(), true);
        Assert.assertEquals(sl.isResponseLine(), false);

        RequestLine rl = sl;
        Assert.assertEquals(rl.getAbsReqUri_host(), null);
        Assert.assertEquals(rl.getAbsReqUri_port(), -1);
        Assert.assertEquals(rl.getAbsReqUri_scheme(), null);
        Assert.assertEquals(rl.getAbsReqUriUsesDefaultPort(), false);
        Assert.assertEquals(rl.getHttpDecl(), "HTTP/1.1");
        Assert.assertEquals(rl.getHttpVer(), "1.1");
        Assert.assertEquals(rl.getMethod(), "GET");
        Assert.assertEquals(rl.getReqFragment(), "fragment");
        Assert.assertEquals(rl.getReqPath(), "/some/url");
        Assert.assertEquals(rl.getReqQuery(), "box=1&sam=2|3&jane=4");
        Assert.assertEquals(rl.getUri(), "/some/url?box=1&sam=2|3&jane=4#fragment");
    }


    @Test
    public void test_tokens_RequestLine() throws MalformedURLException {
        StartLine sl = new StartLine("GET", "/some/url?box=1&sam=2|3&jane=4#fragment", "HTTP/1.1");

        Assert.assertEquals(sl.isAbsReqURI(), false);
        Assert.assertEquals(sl.isBadLine(), false);
        Assert.assertEquals(sl.isRequestLine(), true);
        Assert.assertEquals(sl.isResponseLine(), false);

        RequestLine rl = sl;
        Assert.assertEquals(rl.getAbsReqUri_host(), null);
        Assert.assertEquals(rl.getAbsReqUri_port(), -1);
        Assert.assertEquals(rl.getAbsReqUri_scheme(), null);
        Assert.assertEquals(rl.getAbsReqUriUsesDefaultPort(), false);
        Assert.assertEquals(rl.getHttpDecl(), "HTTP/1.1");
        Assert.assertEquals(rl.getHttpVer(), "1.1");
        Assert.assertEquals(rl.getMethod(), "GET");
        Assert.assertEquals(rl.getReqFragment(), "fragment");
        Assert.assertEquals(rl.getReqPath(), "/some/url");
        Assert.assertEquals(rl.getReqQuery(), "box=1&sam=2|3&jane=4");
        Assert.assertEquals(rl.getUri(), "/some/url?box=1&sam=2|3&jane=4#fragment");
    }

	@Test
	public void testResponseLine() throws MalformedURLException {
		StartLine sl = new StartLine("HTTP/1.1 200 OK");
		
		Assert.assertEquals(sl.isAbsReqURI(), false);
		Assert.assertEquals(sl.isBadLine(), false);
		Assert.assertEquals(sl.isRequestLine(), false);
		Assert.assertEquals(sl.isResponseLine(), true);
		
		ResponseLine rl = sl;
		Assert.assertEquals(rl.getHttpDecl(), "HTTP/1.1");
        Assert.assertEquals(rl.getHttpVer(), "1.1");
        Assert.assertEquals(rl.getMsg(), "OK");
        Assert.assertEquals(rl.getRespCode(), "200");
	}
}
