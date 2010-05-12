package org.lds.sso.appwrap.proxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.testng.Assert;
import org.testng.annotations.Test;

public class HeaderTest {

    @Test
    public void test_CompareToNull() {
        Header hb = new Header(HeaderDef.Accept, "222");
        // should throw NPE per Comparable javadocs.
        try {
            hb.compareTo(null);
            Assert.fail("should have thrown NullPointerException");
        }
        catch(NullPointerException npe) {
        }
    }

    @Test
    public void test_CompareToDefIdentical() {
        Header hb = new Header(HeaderDef.Accept, "222");
        Header ha = new Header(HeaderDef.Accept, "111");
        Assert.assertEquals(hb.compareTo(ha), 0);
    }

    @Test
    public void test_CompareToSameDef() {
        Header hb = new Header(HeaderDef.Accept, "222");
        Header ha = new Header(HeaderDef.AcceptCharset, "111");
        Header hc = new Header(HeaderDef.AcceptEncoding, "444");
        
        Assert.assertEquals(hb.compareTo(ha), HeaderDef.Accept.compareTo(HeaderDef.AcceptCharset));
        Assert.assertEquals(hb.compareTo(hc), HeaderDef.Accept.compareTo(HeaderDef.AcceptEncoding));
        Assert.assertEquals(ha.compareTo(hb), HeaderDef.AcceptCharset.compareTo(HeaderDef.Accept));
        Assert.assertEquals(ha.compareTo(hc), HeaderDef.AcceptCharset.compareTo(HeaderDef.AcceptEncoding));
        Assert.assertEquals(hc.compareTo(hb), HeaderDef.AcceptEncoding.compareTo(HeaderDef.Accept));
        Assert.assertEquals(hc.compareTo(ha), HeaderDef.AcceptEncoding.compareTo(HeaderDef.AcceptCharset));
    }

    @Test
    public void test_EqualsNull() {
        Header hb = new Header(HeaderDef.Accept, "222");
        // as per Comparable javadocs.
        Assert.assertEquals(hb.equals(null), false); 
    }

    @Test
    public void test_EqualsWrongObject() {
        Header hb = new Header(HeaderDef.Accept, "222");
        Object obj = hb;
        Assert.assertEquals(obj.equals(" "), false);
    }

    @Test
    public void test_EqualsDiffDefs() {
        Header hb = new Header(HeaderDef.Accept, "111");
        Header ha = new Header(HeaderDef.AcceptCharset, "111");
        
        Assert.assertEquals(hb.equals(ha), false); // since defs differ
    }

    @Test
    public void test_EqualsExtension() {
        Header hb = new Header("b", "222");
        Header hb2 = new Header("b", "111");
        Header hc = new Header("c", "444");
        
        Assert.assertEquals(hb.equals(hb2), true); // since b equals b
        Assert.assertEquals(hb.equals(hc), false); // since b ! eq c
    }
    
    @Test
    public void test_EqualsSameDefs() {
        Header hb = new Header(HeaderDef.Accept, "222");
        Header ha = new Header(HeaderDef.Accept, "111");
        
        Assert.assertEquals(hb.equals(ha), true);
    }

    @Test
    public void test_HashcodeNonExtension() {
        Header hb = new Header(HeaderDef.Accept, "222");
        
        Assert.assertEquals(hb.hashCode(), HeaderDef.Accept.hashCode());
    }

    @Test
    public void test_HashcodeExtensionObj() {
        Header hb = new Header("b", "222");
        
        Assert.assertEquals(hb.hashCode(), "b".hashCode());
    }

    @Test
    public void test_ToStringExtensionDef() {
        Header hb = new Header("b", "222");
        
        Assert.assertEquals(hb.toString(), "b: 222");
    }

    @Test
    public void test_ToStringNonExtensionDef() {
        Header hb = new Header(HeaderDef.Accept, "222");
        
        Assert.assertEquals(hb.toString(), HeaderDef.Accept.getName() + ": 222");
    }
    

    @Test
    public void test_ExtensionDefWriteTo() throws IOException {
        Header hb = new Header("b", "222");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        hb.writeTo(out);
        Assert.assertEquals(out.toByteArray(), ("b: 222" + RequestHandler.CRLF).getBytes());
    }

    @Test
    public void test_NonExtensionDefWriteTo() throws IOException {
        Header hb = new Header(HeaderDef.Accept, "222");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        hb.writeTo(out);
        Assert.assertEquals(out.toByteArray(), ("Accept: 222" + RequestHandler.CRLF).getBytes());
    }
    
    @Test
    public void test_MapEqualityForSameNonExtDef() {
        Header hb1 = new Header(HeaderDef.Accept, "111");
        Header hb2 = new Header(HeaderDef.Accept, "222");
        
        Map<Header,Header> hdrs = new HashMap<Header,Header>();
        hdrs.put(hb1, hb1);
        
        Assert.assertNotNull(hdrs.get(hb2));
        Assert.assertSame(hdrs.get(hb2), hb1);
    }
    
    @Test
    public void test_MapEqualityForSameNonExtDef2() {
        Header hb1 = new Header(HeaderDef.Accept, "111");
        Header hb2 = new Header(HeaderDef.Accept, "");
        
        Map<Header,Header> hdrs = new HashMap<Header,Header>();
        hdrs.put(hb1, hb1);
        
        Assert.assertNotNull(hdrs.get(hb2));
        Assert.assertSame(hdrs.get(hb2), hb1);
    }
    
    @Test
    public void test_MapEqualityForExtDefSameName() {
        Header hb1 = new Header("a", "111");
        Header hb2 = new Header("a", "222");
        
        Map<Header,Header> hdrs = new HashMap<Header,Header>();
        hdrs.put(hb1, hb1);
        
        Assert.assertNotNull(hdrs.get(hb2));
        Assert.assertSame(hdrs.get(hb2), hb1);
    }
    
    @Test
    public void test_MapInEqualityForExtDefDiffName() {
        Header hb1 = new Header("a", "111");
        Header hb2 = new Header("b", "222");
        
        Map<Header,Header> hdrs = new HashMap<Header,Header>();
        hdrs.put(hb1, hb1);
        
        Assert.assertNull(hdrs.get(hb2));
    }

    
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_ExtThrowsExcp() {
        Header h1 = new Header(HeaderDef.Extension, "111");
        Assert.fail("shuold have thrown exception.");
    }
    
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_NullDefThrowsExcp() {
        Header h1 = new Header((HeaderDef)null, "111");
        Assert.fail("shuold have thrown exception.");
    }
    
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_EmptyNameThrowsExcp() {
        Header h1 = new Header("", "111");
        Assert.fail("shuold have thrown exception.");
    }
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_NullNameThrowsExcp() {
        Header h1 = new Header((String)null, "111");
        Assert.fail("shuold have thrown exception.");
    }
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_NullValThrowsExcp() {
        Header h1 = new Header("sss", (String)null);
        Assert.fail("shuold have thrown exception.");
    }
    
    @Test
    public void test_ExtSortOrderByName() {
        Header h9 = new Header("m", "555");
        Header h10 = new Header("l", "555");
        Header h11 = new Header("k", "555");
        Header h12 = new Header("j", "555");
        
        Map<Header,Header> hdrs = new TreeMap<Header,Header>();
        hdrs.put(h9,h9);
        hdrs.put(h10,h10);
        hdrs.put(h11,h11);
        hdrs.put(h12,h12);
        
        Iterator<Header> itr = hdrs.keySet().iterator();
        Assert.assertSame(itr.next(), h12);
        Assert.assertSame(itr.next(), h11);
        Assert.assertSame(itr.next(), h10);
        Assert.assertSame(itr.next(), h9);
    }
    
}
