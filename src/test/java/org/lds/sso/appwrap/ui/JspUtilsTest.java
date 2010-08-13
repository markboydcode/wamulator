package org.lds.sso.appwrap.ui;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests of the JspUtils class which is a really weird looking animal if you
 * don't understand that its sole purpose is to provide functionality to jsp
 * pages allowing them to pass values in and receive altered values back to be
 * embedded within the page. To accomplish this it uses the map mechanism 
 * defined in jsp allowing lookup of map entries based upon values being 
 * processed within the page. And by receiving such keys we can then take
 * action upon them and return what we wish to implement the needed functionality. 
 * 
 * @author BOYDMR
 *
 */
public class JspUtilsTest {

    @Test
    public void test_getIsSsoDefinedHeader() {
        JspUtils util = new JspUtils();
        Map<String, Boolean> obj = util.getIsSsoDefinedHeader();
        Assert.assertTrue(obj.get(UserHeaderNames.BIRTH_DATE));

        Map<String, Boolean> obj2 = util.getIsSsoDefinedHeader();
        Assert.assertSame(obj, obj2);
        Assert.assertFalse(obj2.get("some-completely-random-string"));
    }

    @Test
    public void test_getCrlfToBr() {
        JspUtils util = new JspUtils();
        Map<String, String> obj = util.getCrlfToBr();
        Assert.assertEquals(obj.get("1\r\n2"), "1<br/>2");

        Map<String, String> obj2 = util.getCrlfToBr();
        Assert.assertSame(obj, obj2);
        Assert.assertEquals(obj2.get("123"), "123");
    }
    @Test
    public void test_getEncode() throws UnsupportedEncodingException {
        JspUtils util = new JspUtils();
        Map<String, String> obj = util.getEncode();
        // doing twice is intentional to test initialization and use thereafter
        Assert.assertEquals(obj.get("http://somehost:80/path;x=large/page.html?a=b&c=d"), 
        "http%3A%2F%2Fsomehost%3A80%2Fpath%3Bx%3Dlarge%2Fpage.html%3Fa%3Db%26c%3Dd");

        Map<String, String> obj2 = util.getEncode();
        Assert.assertSame(obj, obj2);
        Assert.assertEquals(obj2.get("http://somehost:80/path;x=large/page.html?a=b&c=d"), 
        "http%3A%2F%2Fsomehost%3A80%2Fpath%3Bx%3Dlarge%2Fpage.html%3Fa%3Db%26c%3Dd");
    }

    @Test
    public void test_getDecode() {
        JspUtils util = new JspUtils();
        Map<String, String> obj = util.getDecode();
        // doing twice is intentional to test initialization and use thereafter
        Assert.assertEquals(obj.get("http%3A%2F%2Fsomehost%3A80%2Fpath%3Bx%3Dlarge%2Fpage.html%3Fa%3Db%26c%3Dd"), 
        "http://somehost:80/path;x=large/page.html?a=b&c=d");

        Map<String, String> obj2 = util.getDecode();
        Assert.assertSame(obj, obj2);
        Assert.assertEquals(obj2.get("http%3A%2F%2Fsomehost%3A80%2Fpath%3Bx%3Dlarge%2Fpage.html%3Fa%3Db%26c%3Dd"), 
        "http://somehost:80/path;x=large/page.html?a=b&c=d");
    }
}
