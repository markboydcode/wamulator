package org.lds.sso.appwrap.model;

import java.util.ArrayList;
import java.util.List;

public class Policy {

	private String name;
	private String url;
	private String queryString;
	private List<String> operations;
	private AuthNScheme authNScheme;
	private AuthZRule authZRule;
	private List<Header> successHeaders;
	private List<Header> failureHeaders;
	private List<Header> inconclusiveHeaders;
	
	
	public Policy(String name) {
		this.name = name;
		successHeaders = new ArrayList<Header>();
		failureHeaders = new ArrayList<Header>();
		inconclusiveHeaders = new ArrayList<Header>();
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getQueryString() {
		return queryString;
	}
	
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}
	
	public List<String> getOperations() {
		return operations;
	}
	
	public void setOperations(List<String> operations) {
		this.operations = operations;
	}
	
	public AuthNScheme getAuthNScheme() {
		return authNScheme;
	}
	
	public void setAuthNScheme(AuthNScheme authNScheme) {
		this.authNScheme = authNScheme;
	}
	
	public AuthZRule getAuthZRule() {
		return authZRule;
	}
	
	public void setAuthZRule(AuthZRule authZRule) {
		this.authZRule = authZRule;
	}
	
	public List<Header> getSuccessHeaders() {
		return successHeaders;
	}
	
	public void setSuccessHeaders(List<Header> successHeaders) {
		this.successHeaders = successHeaders;
	}
	
	public void addSuccessHeader(Header header) {
		successHeaders.add(header);
	}
	
	public List<Header> getFailureHeaders() {
		return failureHeaders;
	}
	
	public void setFailureHeaders(List<Header> failureHeaders) {
		this.failureHeaders = failureHeaders;
	}
	
	public void addFailureHeader(Header header) {
		inconclusiveHeaders.add(header);
	}
	
	public List<Header> getInconclusiveHeaders() {
		return inconclusiveHeaders;
	}
	
	public void setInconclusiveHeaders(List<Header> inconclusiveHeaders) {
		this.inconclusiveHeaders = inconclusiveHeaders;
	}
	
	public void addInconclusiveHeader(Header header) {
		inconclusiveHeaders.add(header);
	}
}
