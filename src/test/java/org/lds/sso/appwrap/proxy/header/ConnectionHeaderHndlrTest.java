package org.lds.sso.appwrap.proxy.header;

import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.HttpPackageType;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ConnectionHeaderHndlrTest {

    @Test
    public void verifyAppliesToReqAndResp() {
        ConnectionHeaderHndlr hnd = new ConnectionHeaderHndlr();
        Assert.assertTrue(hnd.appliesTo(HeaderDef.Connection.getLcName(), HttpPackageType.REQUEST));
        Assert.assertTrue(hnd.appliesTo(HeaderDef.Connection.getLcName(), HttpPackageType.RESPONSE));
    }

    @Test
    public void verifyCleanup() {
        ConnectionHeaderHndlr hnd = new ConnectionHeaderHndlr();
        HttpPackage pkg = new HttpPackage();
        pkg.isPersistent = true;
        pkg.headerBfr.append(new Header(HeaderDef.KeepAlive, "100"));
        pkg.headerBfr.append(new Header(HeaderDef.Upgrade, "TLS/1.0 HTTP/1.1"));
        pkg.headerBfr.append(new Header("Some-Extenstion", "say goodbye"));
        pkg.headerBfr.append(new Header(HeaderDef.Accept, "text/html"));
        String connHdrVal = HeaderDef.KeepAlive.getName() + " Some-Extenstion "
        + HeaderDef.Upgrade.getName() + " close"; 
        HandlerSet set = HandlerSet.newInstance();
        hnd.handle(HeaderDef.Connection.getLcName(), connHdrVal, pkg, set, null);
        
        Assert.assertEquals(set.secondPhaseActions.size(), 1, "second phase actoin should have been added");
        SecondPhaseAction a = set.secondPhaseActions.get(0);
        a.apply();
        
        Assert.assertFalse(pkg.isPersistent, "'close' should have marked as non-persistent"); 
        Assert.assertNotNull(pkg.headerBfr.getHeader(HeaderDef.Accept), "accept header should have passed through unchanged");
        Assert.assertEquals(pkg.headerBfr.getHeader(HeaderDef.Accept).getValue(), "text/html", "accept header should have passed through unchanged");
        Assert.assertNull(pkg.headerBfr.getHeader(HeaderDef.KeepAlive), "keep-alive should have been stripped");
        Assert.assertNull(pkg.headerBfr.getExtensionHeader("Some-Extenstion"), "some-extension should have been stripped");
        Assert.assertNotNull(pkg.headerBfr.getHeader(HeaderDef.Connection), "connection should have been replaced");
        Assert.assertEquals(pkg.headerBfr.getHeader(HeaderDef.Connection).getValue(), "Upgrade close", "Upgrade should have passed through");
        Assert.assertNotNull(pkg.headerBfr.getHeader(HeaderDef.Upgrade), "Upgrade header should have passed through unchanged");
        Assert.assertEquals(pkg.headerBfr.getHeader(HeaderDef.Upgrade).getValue(), "TLS/1.0 HTTP/1.1", "Upgrade header should have passed through unchanged");
    }

    @Test
    public void verifyCleanupAndInjectOfClose() {
        ConnectionHeaderHndlr hnd = new ConnectionHeaderHndlr();
        HttpPackage pkg = new HttpPackage();
        pkg.isPersistent = true;
        pkg.headerBfr.append(new Header(HeaderDef.KeepAlive, "100"));
        pkg.headerBfr.append(new Header(HeaderDef.Upgrade, "TLS/1.0 HTTP/1.1"));
        pkg.headerBfr.append(new Header("Some-Extenstion", "say goodbye"));
        pkg.headerBfr.append(new Header(HeaderDef.Accept, "text/html"));
        String connHdrVal = HeaderDef.KeepAlive.getName() + " Some-Extenstion "; 
        HandlerSet set = HandlerSet.newInstance();
        hnd.handle(HeaderDef.Connection.getLcName(), connHdrVal, pkg, set, null);
        
        Assert.assertEquals(set.secondPhaseActions.size(), 1, "second phase actoin should have been added");
        SecondPhaseAction a = set.secondPhaseActions.get(0);
        a.apply();
        
        Assert.assertFalse(pkg.isPersistent, "should have marked as non-persistent since we inject close"); 
        Assert.assertNotNull(pkg.headerBfr.getHeader(HeaderDef.Accept), "accept header should have passed through unchanged");
        Assert.assertEquals(pkg.headerBfr.getHeader(HeaderDef.Accept).getValue(), "text/html", "accept header should have passed through unchanged");
        Assert.assertNull(pkg.headerBfr.getHeader(HeaderDef.KeepAlive), "keep-alive should have been stripped");
        Assert.assertNull(pkg.headerBfr.getExtensionHeader("Some-Extenstion"), "some-extension should have been stripped");
        Assert.assertNotNull(pkg.headerBfr.getHeader(HeaderDef.Connection), "connection should have been injected");
        Assert.assertEquals(pkg.headerBfr.getHeader(HeaderDef.Connection).getValue(), "close", "close should have been injected");
        Assert.assertNotNull(pkg.headerBfr.getHeader(HeaderDef.Upgrade), "Upgrade header should have passed through since not tagged via connection header");
        Assert.assertEquals(pkg.headerBfr.getHeader(HeaderDef.Upgrade).getValue(), "TLS/1.0 HTTP/1.1", "Upgrade header should have passed through unchanged");
    }

    @Test
    public void verifyCleanupButNotUpgradeAndInjectOfClose() {
        ConnectionHeaderHndlr hnd = new ConnectionHeaderHndlr();
        HttpPackage pkg = new HttpPackage();
        pkg.headerBfr.append(new Header(HeaderDef.KeepAlive, "100"));
        pkg.headerBfr.append(new Header("Some-Extenstion", "say goodbye"));
        pkg.headerBfr.append(new Header(HeaderDef.Accept, "text/html"));
        String connHdrVal = HeaderDef.KeepAlive.getName() + " Some-Extenstion";
        HandlerSet set = HandlerSet.newInstance();
        hnd.handle(HeaderDef.Connection.getLcName(), connHdrVal, pkg, set, null);
        
        Assert.assertEquals(set.secondPhaseActions.size(), 1, "second phase actoin should have been added");
        SecondPhaseAction a = set.secondPhaseActions.get(0);
        a.apply();

        Assert.assertNotNull(pkg.headerBfr.getHeader(HeaderDef.Accept));
        Assert.assertNull(pkg.headerBfr.getHeader(HeaderDef.KeepAlive));
        Assert.assertNull(pkg.headerBfr.getExtensionHeader("Some-Extenstion"));
        Assert.assertNotNull(pkg.headerBfr.getHeader(HeaderDef.Connection));
        Assert.assertEquals(pkg.headerBfr.getHeader(HeaderDef.Connection).getValue(), "close");
    }
}
