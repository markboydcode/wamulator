package org.lds.sso.appwrap;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SiteMatcherTest {

    @Test
   public void testCctxDeclarativeOrder() throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-traffic>"
            + "  <by-site host='local.lds.org' port='80'>"
            + "    <cctx-mapping cctx='/a/b/c*' thost='127.0.0.1' tport='1' tpath='/a/b/c*'/>"
            + "    <cctx-mapping cctx='/a*' thost='127.0.0.1' tport='2' tpath='/a*'/>"
            + "    <cctx-mapping cctx='/b*' thost='127.0.0.1' tport='3' tpath='/a/b*'/>"
            + "  </by-site>"
            + " </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        TrafficManager tman = cfg.getTrafficManager();
        SiteMatcher m = tman.getSite("local.lds.org", 80);
        EndPoint ep = m.getEndpointForCanonicalUrl("/a/b/c/d");
        Assert.assertEquals(ep.getCanonicalContextRoot(), "/a/b/c", "/a/b/c should be found before /a mapping");
        ep = m.getEndpointForCanonicalUrl("/a/b/d/e");
        Assert.assertEquals(ep.getCanonicalContextRoot(), "/a", "/a should be found before /a/b mapping hiding /a/b");
   }

    @Test
    public void testIllegalCctxDeclarativeOrder() throws Exception {
         String xml = 
             "<?xml version='1.0' encoding='UTF-8'?>"
             + "<config console-port='88' proxy-port='45'>"
             + " <sso-traffic>"
             + "  <by-site host='local.lds.org' port='80'>"
             + "    <cctx-mapping cctx='/a*' thost='127.0.0.1' tport='2' tpath='/a*'/>"
             + "    <cctx-mapping cctx='/a/b/c*' thost='127.0.0.1' tport='1' tpath='/a/b/c*'/>"
             + "  </by-site>"
             + " </sso-traffic>"
             + "</config>";
         Config cfg = new Config();
         try {
             XmlConfigLoader2.load(xml);
             Assert.fail("Should throw IllegalArgumentException since /a/b/c is within /a and hence will never receive requests.");
         }
         catch (Exception i) {
             Throwable t = i.getCause();
             Assert.assertTrue(t != null && t instanceof IllegalArgumentException, "IllegalArgumentException should have been thrown.");
         }
    }
}