package org.lds.sso.appwrap.identity;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.lds.sso.appwrap.identity.legacy.WamulatorUserSource;


public class UserManager {
	protected Map<String, User> users = new TreeMap<String,User>(String.CASE_INSENSITIVE_ORDER);
	private User lastUserAdded;
	
	/**
	 * Defines how values for an attribute values are aggregated when more are 
	 * added in by different user sources when sources are chained.
	 * 
	 * @author BoydMR
	 *
	 */
	public enum Aggregation{
		/**
		 * The values set by a {@link WamulatorUserSource} become fixed and values 
		 * for such an attribute from following stores are ignored.
		 */
		FIX,
		/**
		 * Values for an attribute from all stores are preserved. This is the
		 * default. But two equals values for the same attribute will be reduced to
		 * only one copy.
		 */
		MERGE,
		/**
		 * The values contributed from the last store to contribute 
		 * replace all others.
		 */
		REPLACE;
		
		/**
		 * Returns the value whose name is the same ignoring case or defaults
		 * to {@link MERGE}.
		 * 
		 * @param aggregation
		 * @return
		 */
		public static Aggregation lookup(String aggregation) {
			for(Aggregation a : values()) {
				if (a.name().equalsIgnoreCase(aggregation)) {
					return a;
				}
			}
			return MERGE;
		}
	}
	
	/**
	 * Adds a user to the set of configured users or replaces a user already
	 * there.
	 * 
	 * @param user
	 * @param password
	 * @return 
	 */
	public synchronized User setUser(String username, String password) {
		// first clone the map so we don't get concurrent mod exception
		Map<String, User> copy = new TreeMap<String,User>(String.CASE_INSENSITIVE_ORDER);
		copy.putAll(users);
			
		User usr = copy.get(username);
		
		if (usr == null) {
			usr = new User(username, password);
			copy.put(username, usr);
		}
		else {
			if (!usr.getUsername().equals(username) ) {
				copy.remove(usr.getUsername());
				copy.put(username, usr);
			}
			usr.setUsername(username);
			usr.setPassword(password);
		}
		lastUserAdded = usr;
		// now replace old map 
		users = copy;
		return usr;
	}
	
	/**
	 * Removes the specified user from the store if found. If not found no 
	 * action is taken.
	 * 
	 * @param user
	 */
	public synchronized void removeUser(String username) {
		// first clone the map so we don't get concurrent mod exception
		Map<String, User> copy = new TreeMap<String,User>(String.CASE_INSENSITIVE_ORDER);
		copy.putAll(users);
		copy.remove(username);
		users = copy;
	}

	/**
	 * Validates that the passed-in password matches that had for the user.
	 * 
	 * @param usr
	 * @param pwd
	 * @return
	 */
	public boolean isValidPassword(String username, String pwd) {
		User usr = users.get(username);
		return usr != null && usr.getPassword().equals(pwd);
	}

	
	/**
	 * Returns the set of usernames.
	 * 
	 * @return
	 */
	public Set<String> getUsernames() {
		return Collections.unmodifiableSet(users.keySet());
	}
	
	public Collection<User> getUsers() {
		return Collections.unmodifiableCollection(users.values());
	}

	public int getNumberOfUsers() {
		return users.size();
	}

	/**
	 * Gets the User object for the given username or null if not found.
	 * 
	 * @param username
	 * @return
	 */
	public User getUser(String username) {
		if (username == null) {
			return null;
		}
		return users.get(username);
	}

	/**
	 * Set values for an attribute subject to aggregation rules for the 
	 * attribute defaulting to {@link Aggregation#MERGE} if the attribute has
	 * not yet been defined.
	 * 
	 * @param name
	 * @param values
	 */
    public void addAttributeValuesForLastUserAdded(String name, String[] values) {
    	addAttributeValuesForLastUserAdded(name, values, null);
    }
    
    /**
	 * Set values for an attribute subject to aggregation rules for the 
	 * attribute and setting its {@link Aggregation} strategy if the attribute has
	 * not yet been defined. If null is passed in for the aggregation strategy
	 * then it defaults to {@link Aggregation#MERGE}.
     * 
     * @param name
     * @param values
     * @param aggregation
     */
    public void addAttributeValuesForLastUserAdded(String name, String[] values, Aggregation ag) {
        if (lastUserAdded != null && name != null && values != null
                && ! "".equals(name) && ! "".equals(values)) {
            name = name.trim();
            lastUserAdded.addAttributeValues(name, values, ag);
        }
    }
}