package org.lds.sso.appwrap.xml;

import java.util.ArrayList;
import java.util.List;

public class AliasHolder {
	private List<Alias> aliases = new ArrayList<Alias>();
	
	public void addAlias(Alias alias) {
		aliases.add(alias);
	}
	
	public Alias getAlias(String name) {
		for ( Alias alias : aliases ) {
			if ( alias.name.equals(name) ) {
				return alias;
			}
		}
		return null;
	}
	
	public String getAliasValue(String name) {
		Alias alias = getAlias(name);
		return alias == null ? null : alias.value;
	}
	
	public boolean containsAlias(String name) {
		return getAliasValue(name) != null;
	}
}
