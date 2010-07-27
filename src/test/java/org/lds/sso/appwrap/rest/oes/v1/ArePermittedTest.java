package org.lds.sso.appwrap.rest.oes.v1;

import org.apache.log4j.Level;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ArePermittedTest {

    @Test
    public void test_FixUpUrn() throws Exception {
        ArePermitted are = new ArePermitted("/some/path/arePermitted", "local.lds.org");
        
        Assert.assertEquals(are.fixupUrn("/a/slash/prefixed/resource"), "local.lds.org/a/slash/prefixed/resource");
        Assert.assertEquals(are.fixupUrn("domain/non/slash/prefixed/resource"), "local.lds.org/non/slash/prefixed/resource");
        Assert.assertEquals(are.fixupUrn("a-no-slash-resource"), "local.lds.org/a-no-slash-resource");
    }

    @Test
    public void test_FixUpLinks() throws Exception {
        _FixUpLinks();
        Level old  = ArePermitted.cLog.getLevel();
        ArePermitted.cLog.setLevel(Level.INFO);
        _FixUpLinks();
        ArePermitted.cLog.setLevel(old);
    }

    public void _FixUpLinks() throws Exception {
        Level old  = ArePermitted.cLog.getLevel();
        ArePermitted are = new ArePermitted(                                "/some/path/arePermitted", "local.lds.org");
        
        String s = "some.domain/chars/test/!@#$%^&*()-+={}[]:;\\\"'<>.,/";
        System.out.println("---> " + s.replaceAll("[^0-1a-zA-Z.]", "_"));
        
        Assert.assertEquals(are.fixupLinkUri(                         "http://some.domain/a/resource"), "local.lds.org/LINK/some.domain_a_resource");
        Assert.assertEquals(are.fixupLinkUri(                        "https://some.domain/a/resource"), "local.lds.org/LINK/some.domain_a_resource");
        Assert.assertEquals(are.fixupLinkUri(    "http://some.domain/a/resource?with=query&parms=true"), "local.lds.org/LINK/some.domain_a_resource");
        Assert.assertEquals(are.fixupLinkUri(   "https://some.domain/a/resource?with=query&parms=true"), "local.lds.org/LINK/some.domain_a_resource");
        Assert.assertEquals(are.fixupLinkUri(    "http://some.domain/chars/!@#$%^&*()-+={}[]:\"'<>.,/"), "local.lds.org/LINK/some.domain_chars_______________________.__");

        Assert.assertEquals(are.fixupLinkUri(                                    "http://some.domain"), "local.lds.org/LINK/some.domain");
        Assert.assertEquals(are.fixupLinkUri(                                   "https://some.domain"), "local.lds.org/LINK/some.domain");
        Assert.assertEquals(are.fixupLinkUri(                                    "http://some.domain/"), "local.lds.org/LINK/some.domain_");
        Assert.assertEquals(are.fixupLinkUri(                                   "https://some.domain/"), "local.lds.org/LINK/some.domain_");
        Assert.assertEquals(are.fixupLinkUri(              "http://some.domain?with=query&parms=true"), "local.lds.org/LINK/some.domain");
        Assert.assertEquals(are.fixupLinkUri(             "https://some.domain?with=query&parms=true"), "local.lds.org/LINK/some.domain");
        ArePermitted.cLog.setLevel(old);
    }
}
