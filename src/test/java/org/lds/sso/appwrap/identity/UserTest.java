package org.lds.sso.appwrap.identity;

import org.lds.sso.appwrap.identity.UserManager.Aggregation;
import org.testng.Assert;
import org.testng.annotations.Test;

public class UserTest {

	// tests fix for WAMULAT-71
    @Test
    public void test_change_cn_att() {
        User usr = new User("usr", "pwd");
        usr.setUsername("usr2");
        Assert.assertNotNull(usr.getAttributes());
        Assert.assertEquals(usr.getAttributes().length, 1);

        Assert.assertNotNull(usr.getAttribute(User.ATT_CN));
        Assert.assertEquals(usr.getAttribute(User.ATT_CN).length, 1);
        Assert.assertTrue(usr.hasAttributeValue(User.ATT_CN, "usr2"));
    }

	// tests fix for WAMULAT-71
    @Test
    public void test_has_cn_att_automatically() {
        User usr = new User("usr", "pwd");

        Assert.assertNotNull(usr.getAttributes());
        Assert.assertEquals(usr.getAttributes().length, 1);

        Assert.assertNotNull(usr.getAttribute(User.ATT_CN));
        Assert.assertEquals(usr.getAttribute(User.ATT_CN).length, 1);
        Assert.assertTrue(usr.hasAttributeValue(User.ATT_CN, "usr"));
    }

    @Test
    public void test_multivalued_atts() {
        User usr = new User("usr", "pwd");
        usr.addAttributeValues("0", new String[] {"0-one", "0-two"}, null);
        usr.addAttributeValues("1", new String[] {"one", "two", "three", "four"}, null);
        usr.addAttributeValues("3", new String[] {"3-one", "3-two", "3-three"}, null);

        Assert.assertNotNull(usr.getAttributes());
        // 10 is correct because 'cn' attribute is added automatically
        Assert.assertEquals(usr.getAttributes().length, 10);

        Assert.assertNotNull(usr.getAttribute("0"));
        Assert.assertEquals(usr.getAttribute("0").length, 2);
        Assert.assertTrue(usr.hasAttributeValue("0", "0-one"));
        Assert.assertTrue(usr.hasAttributeValue("0", "0-two"));

        Assert.assertNotNull(usr.getAttribute("1"));
        Assert.assertEquals(usr.getAttribute("1").length, 4);
        Assert.assertTrue(usr.hasAttributeValue("1", "one"));
        Assert.assertTrue(usr.hasAttributeValue("1", "two"));    
        Assert.assertTrue(usr.hasAttributeValue("1", "three"));
        Assert.assertTrue(usr.hasAttributeValue("1", "four"));

        Assert.assertNotNull(usr.getAttribute("3"));
        Assert.assertEquals(usr.getAttribute("3").length, 3);
        Assert.assertTrue(usr.hasAttributeValue("3", "3-one"));
        Assert.assertTrue(usr.hasAttributeValue("3", "3-two"));
        Assert.assertTrue(usr.hasAttributeValue("3", "3-three"));
    }

    
    /**
     * Verifies two things: if additional values are injected they are added to
     * what is already there, if any values are added that already exist they
     * have no impact meaning duplicate identical values will never be found. 
     * 
     */
    @Test
    public void test_merge_aggregation() {
        User usr = new User("usr", "pwd");
        usr.addAttributeValues("0", new String[] {"c", "a"}, Aggregation.MERGE);

        Assert.assertNotNull(usr.getAttribute("0"));
        Assert.assertEquals(usr.getAttribute("0").length, 2);
        Assert.assertTrue(usr.hasAttributeValue("0", "a"));
        Assert.assertTrue(usr.hasAttributeValue("0", "c"));
        Assert.assertEquals(usr.getAttribute("0")[0], "a", "should be alphabetical");
        Assert.assertEquals(usr.getAttribute("0")[1], "c", "should be alphabetical");

        usr.addAttributeValues("0", new String[] {"a", "b", "c", "d"});
        Assert.assertEquals(usr.getAttribute("0").length, 4);
        Assert.assertEquals(usr.getAttribute("0")[0], "a", "should be alphabetical");
        Assert.assertEquals(usr.getAttribute("0")[1], "b", "should be alphabetical");
        Assert.assertEquals(usr.getAttribute("0")[2], "c", "should be alphabetical");
        Assert.assertEquals(usr.getAttribute("0")[3], "d", "should be alphabetical");
    }

    @Test
    public void test_hasAttribute() {
        User usr = new User("usr", "pwd");
        Assert.assertFalse(usr.hasAttribute("3"));
        Assert.assertFalse(usr.hasAttribute(null));

        usr.addAttributeValues(null, new String[] {"one"}, null);
        Assert.assertTrue(usr.hasAttribute(null));

        usr.addAttributeValues("3", new String[] {null}, null);
        Assert.assertTrue(usr.hasAttribute("3"));
    }
}
