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
}
