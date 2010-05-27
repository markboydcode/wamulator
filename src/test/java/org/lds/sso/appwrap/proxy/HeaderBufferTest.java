package org.lds.sso.appwrap.proxy;

import java.util.Iterator;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

public class HeaderBufferTest {

    @Test
    public void test_append_Set_Remove_get() {
        HeaderBuffer buf = new HeaderBuffer();
        Header hb = new Header(HeaderDef.Accept, "222");
        buf.append(hb);
        
        Assert.assertNotNull(buf.getHeader(HeaderDef.Accept));
        Assert.assertEquals(buf.getHeader(HeaderDef.Accept).getValue(), "222");

        Header ha = new Header(HeaderDef.Accept, "111");
        buf.append(ha);

        Assert.assertNotNull(buf.getHeader(HeaderDef.Accept));
        Assert.assertEquals(buf.getHeader(HeaderDef.Accept).getValue(), "222");
        Assert.assertNotNull(buf.getHeaders(HeaderDef.Accept));
        Assert.assertEquals(buf.getHeaders(HeaderDef.Accept).size(), 2);
        Assert.assertEquals(buf.getHeaders(HeaderDef.Accept).get(0).getValue(), "222");
        Assert.assertEquals(buf.getHeaders(HeaderDef.Accept).get(1).getValue(), "111");
        
        Header h3 = new Header(HeaderDef.Accept, "333");
        List<Header> removed = buf.set(h3);

        // test if in order added originally
        Assert.assertNotNull(removed);
        Assert.assertEquals(removed.size(), 2);
        Assert.assertEquals(removed.get(0).getValue(), "222");
        Assert.assertEquals(removed.get(1).getValue(), "111");

        Assert.assertNotNull(buf.getHeader(HeaderDef.Accept));
        Assert.assertEquals(buf.getHeader(HeaderDef.Accept).getValue(), "333");
        Assert.assertNotNull(buf.getHeaders(HeaderDef.Accept));
        Assert.assertEquals(buf.getHeaders(HeaderDef.Accept).size(), 1);
        Assert.assertEquals(buf.getHeaders(HeaderDef.Accept).get(0).getValue(), "333");

        removed = buf.removeHeader(HeaderDef.Accept);
        
        Assert.assertNotNull(removed);
        Assert.assertEquals(removed.size(), 1);
        Assert.assertEquals(removed.get(0).getValue(), "333");

        Assert.assertNull(buf.getHeader(HeaderDef.Accept));
        Assert.assertNull(buf.getHeaders(HeaderDef.Accept));
        
        removed = buf.removeHeader(HeaderDef.Accept);
        
        Assert.assertNull(removed);
        Assert.assertNull(buf.getHeader(HeaderDef.Accept));
        Assert.assertNull(buf.getHeaders(HeaderDef.Accept));
        
        Assert.assertNull(buf.getHeaders(null));
        Assert.assertNull(buf.getHeaders(HeaderDef.Extension));
        Assert.assertNull(buf.removeHeader(HeaderDef.Extension));
        Assert.assertNull(buf.removeHeader(null));
    }
    
    @Test
    public void test_RemoveOnlyGetsMatching() {
        HeaderBuffer buf = new HeaderBuffer();
        Header hb = new Header(HeaderDef.Accept, "222");
        buf.append(hb);
        Header ha = new Header(HeaderDef.AcceptCharset, "111");
        buf.append(ha);
        Header hc = new Header(HeaderDef.Age, "111");
        buf.append(hc);

        List<Header> removed = buf.removeHeader(HeaderDef.Accept);
        
        Assert.assertNotNull(removed);
        Assert.assertEquals(removed.size(), 1);
        Assert.assertSame(removed.get(0), hb);
    }
    
    @Test
    public void test_removeExtHeader() {
        HeaderBuffer buf = new HeaderBuffer();
        Header hb = new Header("b", "222");
        buf.append(hb);
        
        Assert.assertNull(buf.getExtensionHeader("a"));
        Assert.assertNotNull(buf.getExtensionHeader("b"));
        Assert.assertSame(buf.getExtensionHeader("b"), hb);
        List<Header> hdrs = buf.getExtensionHeaders("b");
        Assert.assertNotNull(hdrs);
        Assert.assertEquals(hdrs.size(), 1);
        Assert.assertSame(hdrs.get(0), hb);
        
        hdrs = buf.removeExtensionHeader("b");

        Assert.assertNotNull(hdrs);
        Assert.assertEquals(hdrs.size(),1);
        Assert.assertSame(hdrs.get(0), hb);
        
        Assert.assertNull(buf.getExtensionHeader(null));
        Assert.assertNull(buf.getExtensionHeader("accept"));
        Assert.assertNull(buf.getExtensionHeaders(null));
        Assert.assertNull(buf.getExtensionHeaders("accept"));
        Assert.assertNull(buf.removeExtensionHeader(null));
        Assert.assertNull(buf.removeExtensionHeader("accept"));
    }

    @Test
    public void test_iteratorAndToString() {
        HeaderBuffer buf = new HeaderBuffer();
        Header hb1 = new Header("b", "333");
        Header hb2 = new Header("a", "222");
        Header ha = new Header(HeaderDef.Accept, "111");
        Header hc = new Header(HeaderDef.Connection,"444");
        buf.append(hb1);
        buf.append(hb2);
        buf.append(ha);
        buf.append(hc);
        
        String s = buf.toString();
        Assert.assertEquals(s, 
                "HeaderBuffer [\r\n" + 
        		"b: 333,\r\n" + 
        		"a: 222,\r\n" + 
        		"Accept: 111,\r\n" + 
        		"Connection: 444\r\n" + 
        		"]");

        Iterator<Header> itr = buf.getIterator();
        
        Assert.assertSame(itr.next(), hb1);
        Assert.assertSame(itr.next(), hb2);
        Assert.assertSame(itr.next(), ha);
        Assert.assertSame(itr.next(), hc);
    }
}
