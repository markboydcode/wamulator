package org.lds.sso.appwrap.identity;

import org.lds.sso.appwrap.identity.legacy.WamulatorUserSource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


public class UserManager {
	protected Map<String, User> users = new TreeMap<String,User>(String.CASE_INSENSITIVE_ORDER);

	/**
	 * Should only be used for user stores that load their user values at wamulator startup time ensuring only a
	 * single thread is loading users. User stores that do just-in-time provisioning such as the ldap user source
	 * should use the setUser method that accepts attributes to replace a user and its attributes in a single,
	 * thread-safe call.
	 */
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
		 * to {@link org.lds.sso.appwrap.identity.UserManager.Aggregation#MERGE}.
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
	 * there. Should only be used at start up time when single thread loading can be ensured since attributes are
	 * added at a later point in time.
	 *
	 * @param username
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
	 * Adds a user to the set of configured users or replaces an existing user object already
	 * there. Replacement of attributes is determined by the aggregationStrategy: {@link org.lds.sso.appwrap
	 * .identity.UserManager.Aggregation#REPLACE} replaces all attribute values, {@link org.lds.sso.appwrap
	 * .identity.UserManager.Aggregation#MERGE} adds to the exsting attribute values already found. Password is
	 * always replaced if changed.
	 *
	 * @param username
	 * @param password
	 * @param aggregationStrategy
	 * @return
	 */
	public synchronized User setUser(String username, String password, Map<String, List<String>> attributes, Aggregation aggregationStrategy) {
		// first clone the map so we don't get concurrent mod exception
		Map<String, User> copy = new TreeMap<String,User>(String.CASE_INSENSITIVE_ORDER);
		copy.putAll(users);
		User usr;
		User old = copy.get(username);

		if (aggregationStrategy == Aggregation.MERGE) {
			usr = old;
			usr.setPassword(password);
		}
		else {
			usr = new User(username, password);
		}

		for(Map.Entry<String, List<String>> ent : attributes.entrySet()) {
			usr.addAttributeValues(ent.getKey(), ent.getValue().toArray(ExternalUserSource.STRING_ARRAY), aggregationStrategy);
		}
		copy.put(username, usr);

		// now replace old map
		users = copy;
		return usr;
	}

	/**
	 * Removes the specified user from the store if found. If not found no 
	 * action is taken.
	 * 
	 * @param username
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
	 * @param username
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
     * @param ag
     */
    public void addAttributeValuesForLastUserAdded(String name, String[] values, Aggregation ag) {
        if (lastUserAdded != null && name != null && values != null
                && ! "".equals(name) && ! "".equals(values)) {
            name = name.trim();
            lastUserAdded.addAttributeValues(name, values, ag);
        }
    }
}