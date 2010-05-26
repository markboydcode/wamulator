package org.lds.sso.appwrap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.testng.Assert;
import org.testng.annotations.Test;

public class EmptyQueryParmTest {

    @Test
    public void test() throws Exception {
        
        String[] toks = "a=b&c=d&g&d=f".split("&");
        
        int len = toks.length;
        String endpoint = "http://local.lds.org/public/debug.jsp";
        
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(endpoint);
        method.setFollowRedirects(false);
        NameValuePair nvp = new NameValuePair();
        nvp.setName("name-only");
        NameValuePair[] pairs = new NameValuePair[] { 
                new NameValuePair("start", null),
                new NameValuePair("middle", null),
                nvp,
                new NameValuePair("name", "value"),
                new NameValuePair("end", null)
        };
        method.setQueryString(pairs);
        int status = client.executeMethod(method);
        System.out.println("status = " + status);
        System.out.println(method.getResponseBodyAsString());
    }
    

}
