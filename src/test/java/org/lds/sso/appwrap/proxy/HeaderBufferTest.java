package org.lds.sso.appwrap.proxy;

import java.util.Iterator;

import org.testng.Assert;
import org.testng.annotations.Test;

public class HeaderBufferTest {

    @Test
    public void test_append() {
        HeaderBuffer buf = new HeaderBuffer();
        Header hb = new Header(HeaderDef.Accept, "222");
        buf.append(hb);
        
        Assert.assertNotNull(buf.getHeader(HeaderDef.Accept));
        Assert.assertEquals(buf.getHeader(HeaderDef.Accept).getValue(), "222");

        Header ha = new Header(HeaderDef.Accept, "111");
        buf.append(ha);

        Assert.assertNotNull(buf.getHeader(HeaderDef.Accept));
        Assert.assertEquals(buf.getHeader(HeaderDef.Accept).getValue(), "222,111");
    }

    @Test
    public void test_set() {
        HeaderBuffer buf = new HeaderBuffer();
        Header hb = new Header(HeaderDef.Accept, "222");
        buf.append(hb);
        
        Assert.assertNotNull(buf.getHeader(HeaderDef.Accept));
        Assert.assertSame(buf.getHeader(HeaderDef.Accept), hb);

        Header ha = new Header(HeaderDef.Accept, "111");
        Header hr = buf.set(ha);

        Assert.assertSame(hr, hb);
        Assert.assertNotNull(buf.getHeader(HeaderDef.Accept));
        Assert.assertSame(buf.getHeader(HeaderDef.Accept), ha);
    }

    @Test
    public void test_removeHeader() {
        HeaderBuffer buf = new HeaderBuffer();
        Header hb = new Header(HeaderDef.Accept, "222");
        buf.append(hb);
        
        Assert.assertNotNull(buf.getHeader(HeaderDef.Accept));
        Assert.assertSame(buf.getHeader(HeaderDef.Accept), hb);
        Header hr = buf.removeHeader(HeaderDef.Accept);

        Assert.assertSame(hr, hb);
        Assert.assertNull(buf.getHeader(HeaderDef.Accept));
    }

    @Test
    public void test_removeExtHeader() {
        HeaderBuffer buf = new HeaderBuffer();
        Header hb = new Header("b", "222");
        buf.append(hb);
        
        Assert.assertNull(buf.getHeader(null));
        Assert.assertNull(buf.getHeader(HeaderDef.Accept));
        Assert.assertNotNull(buf.getExtensionHeader("b"));
        Assert.assertSame(buf.getExtensionHeader("b"), hb);
        Header hr = buf.removeExtensionHeader("b");

        Assert.assertSame(hr, hb);
        Assert.assertNull(buf.getExtensionHeader("b"));
    }

    @Test
    public void test_iterator() {
        HeaderBuffer buf = new HeaderBuffer();
        Header hb1 = new Header("b", "333");
        Header hb2 = new Header("a", "222");
        Header ha = new Header(HeaderDef.Accept, "111");
        Header hc = new Header(HeaderDef.Connection,"444");
        buf.append(hb1);
        buf.append(hb2);
        buf.append(ha);
        buf.append(hc);
        
        Iterator<Header> itr = buf.getIterator();
        
        Assert.assertSame(itr.next(), hb1);
        Assert.assertSame(itr.next(), hb2);
        Assert.assertSame(itr.next(), ha);
        Assert.assertSame(itr.next(), hc);
    }
}
