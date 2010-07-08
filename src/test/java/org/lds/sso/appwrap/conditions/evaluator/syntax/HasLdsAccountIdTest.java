package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Principal;
import java.util.TreeMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.easymock.classextension.EasyMock;
import org.lds.sso.appwrap.User;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HasLdsAccountIdTest extends TestBaseClass {
	
    @Test
    public void test_init() throws Exception {
        try {
            new HasLdsAccountId().init(null, null);
            Assert.fail("Should have thrown IllegalArgumentException.");
        }
        catch(IllegalArgumentException e) {
        }
    }

    @Test
    public void test_isConditionSatisfied() throws Exception {
        try {
            new HasLdsAccountId().isConditionSatisfied(null);
            Assert.fail("Should have thrown UnsupportedOperationException.");
        }
        catch(UnsupportedOperationException e) {
        }
    }

    @Test
    public void test_addEvaluator() throws Exception {
        try {
            new HasLdsAccountId().addEvaluator(null);
            Assert.fail("Should have thrown UnsupportedOperationException.");
        }
        catch(UnsupportedOperationException e) {
        }
    }

    @Test
    public void test_validate() throws Exception {
        try {
            new HasLdsAccountId().validate();
            Assert.fail("Should have thrown UnsupportedOperationException.");
        }
        catch(UnsupportedOperationException e) {
        }
    }
}
