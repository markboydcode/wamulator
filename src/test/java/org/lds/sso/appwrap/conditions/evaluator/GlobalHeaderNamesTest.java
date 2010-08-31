package org.lds.sso.appwrap.conditions.evaluator;

import java.net.MalformedURLException;

import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.StartLine;
import org.testng.Assert;
import org.testng.annotations.Test;

public class GlobalHeaderNamesTest {

    @Test
    public void test_signMeInDetection() throws MalformedURLException {
        assertSignmein("/path?signmein",  "/path");
        assertSignmein("/path?signmein=", "/path");
        assertSignmein("/path?signmein&a=b",  "/path?a=b");
        assertSignmein("/path?signmein=&a=b", "/path?a=b");
        assertSignmein("/path?a=b&signmein",  "/path?a=b");
        assertSignmein("/path?a=b&signmein=", "/path?a=b");
        assertSignmein("/path?a=b&signmein&c=d", "/path?a=b&c=d");
        assertSignmein("/path?a=b&signmein=&c=d", "/path?a=b&c=d");
        assertNoSignmein("/path?a=b&c=d", "/path?a=b&c=d");
        assertNoSignmein("/path;signmein=true,false;one=two;three/more/path", "/path;signmein=true,false;one=two;three/more/path");
        Assert.assertFalse(GlobalHeaderNames.detectedAndStrippedSignMeIn(null));
    }

    private void assertNoSignmein(String uri, String result) throws MalformedURLException {
        HttpPackage pkg = new HttpPackage();
        StartLine sl = new StartLine("n/a " + uri + " n/a");
        pkg.requestLine = sl;
        boolean a = GlobalHeaderNames.detectedAndStrippedSignMeIn(pkg);
        
        Assert.assertFalse(a, "should not have found signmein signal");
        Assert.assertTrue(pkg.requestLine == sl, "should not have replaced request line");
    }
    
    private void assertSignmein(String uri, String result) throws MalformedURLException {
        HttpPackage pkg = new HttpPackage();
        StartLine sl = new StartLine("n/a " + uri + " n/a");
        pkg.requestLine = sl;
        boolean a = GlobalHeaderNames.detectedAndStrippedSignMeIn(pkg);
        
        Assert.assertTrue(a, "should have found signmein signal");
        Assert.assertFalse(pkg.requestLine == sl, "should have replaced request line");
        Assert.assertEquals(pkg.requestLine.getUri(), result);
    }
    
    @Test
    public void test_signMeOutDetection() throws MalformedURLException {
        assertSignmeout("/path?signmeout",  "/path");
        assertSignmeout("/path?signmeout=", "/path");
        assertSignmeout("/path?signmeout&a=b",  "/path?a=b");
        assertSignmeout("/path?signmeout=&a=b", "/path?a=b");
        assertSignmeout("/path?a=b&signmeout",  "/path?a=b");
        assertSignmeout("/path?a=b&signmeout=", "/path?a=b");
        assertSignmeout("/path?a=b&signmeout&c=d", "/path?a=b&c=d");
        assertSignmeout("/path?a=b&signmeout=&c=d", "/path?a=b&c=d");
        assertNoSignmeout("/path?a=b&c=d", "/path?a=b&c=d");
        assertNoSignmeout("/path;signmeout=true,false;one=two;three/more/path", "/path;signmeout=true,false;one=two;three/more/path");
        Assert.assertFalse(GlobalHeaderNames.detectedAndStrippedSignMeIn(null));
    }

    private void assertNoSignmeout(String uri, String result) throws MalformedURLException {
        HttpPackage pkg = new HttpPackage();
        StartLine sl = new StartLine("n/a " + uri + " n/a");
        pkg.requestLine = sl;
        boolean a = GlobalHeaderNames.detectedAndStrippedSignMeOut(pkg);
        
        Assert.assertFalse(a, "should not have found signmeout signal");
        Assert.assertTrue(pkg.requestLine == sl, "should not have replaced request line");
    }
    
    private void assertSignmeout(String uri, String result) throws MalformedURLException {
        HttpPackage pkg = new HttpPackage();
        StartLine sl = new StartLine("n/a " + uri + " n/a");
        pkg.requestLine = sl;
        boolean a = GlobalHeaderNames.detectedAndStrippedSignMeOut(pkg);
        
        Assert.assertTrue(a, "should have found signmeout signal");
        Assert.assertFalse(pkg.requestLine == sl, "should have replaced request line");
        Assert.assertEquals(pkg.requestLine.getUri(), result);
    }
}
