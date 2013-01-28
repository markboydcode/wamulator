package org.lds.sso.appwrap;

import java.net.URL;

import org.lds.sso.appwrap.AppEndPoint.Scheme;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SiteMatcherTest {

    @Test
   public void testCctxDeclarativeOrder() throws Exception {
    	URL filePath = SiteMatcherTest.class.getClassLoader().getResource("SiteMatcherTestConfig.xml");
    	
    	String xml = 
    		"<?file-alias policy-src-xml=\"" + filePath.getPath().substring(1).replace("/", "\\") + "\"?>"	
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-traffic>"
            + "  <by-site host='local.lds.org' port='80'>"
            + "	   <cctx-mapping thost='127.0.0.1' tport='1' tpath='/'>"
            + "      <policy-source>xml={{policy-src-xml}}</policy-source>"
            + "    </cctx-mapping>"
            + "  </by-site>"
            + " </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        TrafficManager tman = cfg.getTrafficManager();
        SiteMatcher m = tman.getSite(Scheme.HTTP, "local.lds.org", 80);
        EndPoint ep = m.getEndpointForCanonicalUrl("/a/b/c/d");
        Assert.assertEquals(ep.getContextRoot(), "/a/b/c{/.../*,*}", "/a/b/c should be found before /a mapping");
        ep = m.getEndpointForCanonicalUrl("/a/b/d/e");
        Assert.assertEquals(ep.getContextRoot(), "/a{/.../*,*}", "/a should be found");
   }
}
