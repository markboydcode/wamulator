package org.lds.sso.appwrap.model;


public class AuthZRule {

	private String name;
	private boolean enabled;
	private boolean allowTakesPrecedence;
	private Condition allowCondition;
	private Condition denyCondition;
	
	
	public AuthZRule(String name, boolean enabled, boolean allowTakesPrecedence) {
		this.name = name;
		this.enabled = enabled;
		this.allowTakesPrecedence = allowTakesPrecedence;
		this.allowCondition = null;
		this.denyCondition = null;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public boolean allowTakesPrecedence() {
		return allowTakesPrecedence;
	}
	
	public void setAllowTakesPrecedence(boolean allowTakesPrecedence) {
		this.allowTakesPrecedence = allowTakesPrecedence;
	}
	
	public Condition getAllowCondition() {
		return allowCondition;
	}
	
	public void setAllowCondition(Condition allowCondition) {
		this.allowCondition = allowCondition;
	}
	
	public Condition getDenyCondition() {
		return denyCondition;
	}
	
	public void setDenyCondition(Condition denyCondition) {
		this.denyCondition = denyCondition;
	}
	
	
	
	public AuthZRule clone() {
		AuthZRule clone = new AuthZRule(this.getName(), this.isEnabled(), this.allowTakesPrecedence());
		clone.setAllowCondition(this.getAllowCondition());
		clone.setDenyCondition(this.getDenyCondition());
		return clone;
	}
}
