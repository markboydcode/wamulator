package org.lds.sso.appwrap;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.testng.Assert;
import org.testng.annotations.Test;

public class NvPairTest {

    @Test
    public void testCompare() {
        Set<NvPair> set = new TreeSet<NvPair>();
        set.add(new NvPair("abc", "deg"));
        set.add(new NvPair("abc", "dee"));
        set.add(new NvPair("abc", "def"));
        set.add(new NvPair("abb", "def"));
        set.add(new NvPair("acc", "def"));
        set.add(new NvPair(null, null));
        set.add(new NvPair(null, "deg"));
        set.add(new NvPair(null, "dee"));
        
        Assert.assertEquals(set.size(), 8);
        set.add(new NvPair("acc", "def")); // should replace previous one
        Assert.assertEquals(set.size(), 8);
        
        Iterator<NvPair> itr = set.iterator();
        NvPair n = itr.next();
        Assert.assertEquals(n.getName(), null);
        Assert.assertEquals(n.getValue(), null);
        n=itr.next();
        Assert.assertEquals(n.getName(), null);
        Assert.assertEquals(n.getValue(), "dee");
        n=itr.next();
        Assert.assertEquals(n.getName(), null);
        Assert.assertEquals(n.getValue(), "deg");
        n=itr.next();
        Assert.assertEquals(n.getName(), "abb");
        Assert.assertEquals(n.getValue(), "def");
        n=itr.next();
        Assert.assertEquals(n.getName(), "abc");
        Assert.assertEquals(n.getValue(), "dee");
        n=itr.next();
        Assert.assertEquals(n.getName(), "abc");
        Assert.assertEquals(n.getValue(), "def");
        n=itr.next();
        Assert.assertEquals(n.getName(), "abc");
        Assert.assertEquals(n.getValue(), "deg");
        n=itr.next();
        Assert.assertEquals(n.getName(), "acc");
    }
    
    @Test
    public void testEquals() {
        Assert.assertTrue(new NvPair("", "").equals(new NvPair("", "")));
        Assert.assertTrue(new NvPair("a", "b").equals(new NvPair("a", "b")));
        
        Assert.assertTrue(new NvPair(null, null).equals(new NvPair(null, null)));
        
        Assert.assertFalse(new NvPair("a", "").equals(new NvPair("", "a")));
        
        Assert.assertFalse(new NvPair("a", null).equals(new NvPair(null, "a")));
        
        Assert.assertFalse(new NvPair("ab", "cd").equals(new NvPair("a", "bcd")));
    }
}
