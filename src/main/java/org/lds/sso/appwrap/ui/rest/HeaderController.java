package org.lds.sso.appwrap.ui.rest;

import org.lds.sso.appwrap.Config;

import java.util.HashMap;

/**
 * Injects values needed by the header-fragment.ftl included by other templates.
 *
 * Created by markboyd on 5/19/14.
 */
public class HeaderController {

    public static void injectValues(HashMap root, Config cfg) {
        if (cfg.getConsoleTitle() != null && ! "".equals(cfg.getConsoleTitle())) {
            root.put("consoleTitle", cfg.getConsoleTitle());
        }
        else {
            root.put("consoleTitle", "Console: " + cfg.getServerName());
        }
    }
}
