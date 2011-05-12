package org.lds.sso.appwrap.proxy;

import org.lds.sso.appwrap.Config;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ListenerCoordinatorTest {

    private ListenerCoordinator getLC(Config cfg) {
        return new ListenerCoordinator(cfg) {

            @Override
            protected void makeLogDirectory() {
                // don't do in tests
            }

            @Override
            protected void startLogFileCleaner() {
                // don't do in tests
            }
        };
    }

    @Test
    public void test_formatField() {
        ListenerCoordinator c = getLC(Config.getInstance());
        Assert.assertEquals(c.getFormatFieldFor(5), "0");
        Assert.assertEquals(c.getFormatFieldFor(10), "0");
        Assert.assertEquals(c.getFormatFieldFor(100), "00");
        Assert.assertEquals(c.getFormatFieldFor(80), "00");
        Assert.assertEquals(c.getFormatFieldFor(1000), "000");
        Assert.assertEquals(c.getFormatFieldFor(1535), "0000");
    }
    
    @Test
    public void test_getConnectionId_max_not_divisible_by_10() {
        Config cfg = new Config();
        cfg.setMaxEntries(5);
        ListenerCoordinator c = getLC(cfg);
        c.cfg = cfg;
        Assert.assertEquals(c.getConnectionId(), "C-0");
        Assert.assertEquals(c.getConnectionId(), "C-1");
        Assert.assertEquals(c.getConnectionId(), "C-2");
        Assert.assertEquals(c.getConnectionId(), "C-3");
        Assert.assertEquals(c.getConnectionId(), "C-4");
        Assert.assertEquals(c.getConnectionId(), "C-0");
    }
    
    
    @Test
    public void test_getConnectionId_max_not_divisible_by_10_double_digit() {
        Config cfg = new Config();
        cfg.setMaxEntries(14);
        ListenerCoordinator c = getLC(cfg);
        c.cfg = cfg;
        Assert.assertEquals(c.getConnectionId(), "C-00");
        Assert.assertEquals(c.getConnectionId(), "C-01");
        Assert.assertEquals(c.getConnectionId(), "C-02");
        Assert.assertEquals(c.getConnectionId(), "C-03");
        Assert.assertEquals(c.getConnectionId(), "C-04");
        Assert.assertEquals(c.getConnectionId(), "C-05");
        Assert.assertEquals(c.getConnectionId(), "C-06");
        Assert.assertEquals(c.getConnectionId(), "C-07");
        Assert.assertEquals(c.getConnectionId(), "C-08");
        Assert.assertEquals(c.getConnectionId(), "C-09");
        Assert.assertEquals(c.getConnectionId(), "C-10");
        Assert.assertEquals(c.getConnectionId(), "C-11");
        Assert.assertEquals(c.getConnectionId(), "C-12");
        Assert.assertEquals(c.getConnectionId(), "C-13");
    }
    
    @Test
    public void test_getConnectionId_max_divisible_by_10() {
        Config cfg = new Config();
        cfg.setMaxEntries(10);
        ListenerCoordinator c = getLC(cfg);
        c.cfg = cfg;
        Assert.assertEquals(c.getConnectionId(), "C-0");
        Assert.assertEquals(c.getConnectionId(), "C-1");
        Assert.assertEquals(c.getConnectionId(), "C-2");
        Assert.assertEquals(c.getConnectionId(), "C-3");
        Assert.assertEquals(c.getConnectionId(), "C-4");
        Assert.assertEquals(c.getConnectionId(), "C-5");
        Assert.assertEquals(c.getConnectionId(), "C-6");
        Assert.assertEquals(c.getConnectionId(), "C-7");
        Assert.assertEquals(c.getConnectionId(), "C-8");
        Assert.assertEquals(c.getConnectionId(), "C-9");
        Assert.assertEquals(c.getConnectionId(), "C-0");
    }
}
