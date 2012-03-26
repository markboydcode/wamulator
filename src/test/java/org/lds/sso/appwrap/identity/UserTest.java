package org.lds.sso.appwrap.identity;

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
        usr.addAttribute("0", "0-one");
        usr.addAttribute("0", "0-two");

        usr.addAttribute("1", "one");
        usr.addAttribute("1", "two");
        usr.addAttribute("1", "three");
        usr.addAttribute("1", "four");

        usr.addAttribute("3", "3-one");
        usr.addAttribute("3", "3-two");
        usr.addAttribute("3", "3-three");

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

    @Test
    public void test_hasAttribute() {
        User usr = new User("usr", "pwd");
        Assert.assertFalse(usr.hasAttribute("3"));
        Assert.assertFalse(usr.hasAttribute(null));

        usr.addAttribute(null, "one");
        Assert.assertTrue(usr.hasAttribute(null));

        usr.addAttribute("3", null);
        Assert.assertTrue(usr.hasAttribute("3"));
    }
}
