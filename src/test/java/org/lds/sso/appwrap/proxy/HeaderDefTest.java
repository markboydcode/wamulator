package org.lds.sso.appwrap.proxy;

import org.testng.Assert;
import org.testng.annotations.Test;

public class HeaderDefTest {

    @Test
    public void test_CreateNonExtensionHeader() {
        Header h = HeaderDef.createHeader("accept", "111");
        Assert.assertEquals(h.getName(), HeaderDef.Accept.getName());
    }

    @Test
    public void test_CreatExtensionHeader() {
        Header h = HeaderDef.createHeader("undefined-name", "111");
        Assert.assertEquals(h.getName(), "undefined-name");
    }

    @Test
    public void test_CreatHeaderViaDef() {
        Header h = HeaderDef.createHeader(HeaderDef.Connection, "111");
        Assert.assertEquals(h.getName(), HeaderDef.Connection.getName());
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_CreatHeaderViaExtDefThrowsExcp() {
        Header h = HeaderDef.createHeader(HeaderDef.Extension, "111");
        Assert.fail("should have thrown IllegalArgumentException.");
    }
}
