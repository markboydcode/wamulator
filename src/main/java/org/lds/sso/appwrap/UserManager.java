package org.lds.sso.appwrap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class UserManager {
	protected Map<String, User> users = new TreeMap<String,User>();
	private User lastUserAdded;
	
	/**
	 * Adds a user to the set of configured users or replaces a user already
	 * there.
	 * 
	 * @param user
	 * @param password
	 */
	public synchronized void setUser(String username, String password) {
		// first clone the map so we don't get concurrent mod exception
		Map<String, User> copy = new TreeMap<String,User>();
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
	}
	
	/**
	 * Removes the specified user from the store if found. If not found no 
	 * action is taken.
	 * 
	 * @param user
	 */
	public synchronized void removeUser(String username) {
		// first clone the map so we don't get concurrent mod exception
		Map<String, User> copy = new TreeMap<String,User>();
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
	public User  getUser(String username) {
		if (username == null) {
			return null;
		}
		return users.get(username);
	}

	public void addHeader(String username, String name, String value) {
		User usr = users.get(username);
		
		if (usr != null) {
			usr.addHeader(name, value);
		}
	}

	/**
	 * Adds a header for the last user added. The header should NOT end with a 
	 * colon.
	 * 
	 * @param header
	 * @param value
	 * @throws SSOException 
	 */
	public void addHeaderForLastUserAdded(String header, String value) {
		if (lastUserAdded != null && header != null && value != null
				&& ! "".equals(header) && ! "".equals(value)) {
			header = header.trim();
			lastUserAdded.addHeader(header, value);
		}
	}

    public void addAttributeForLastUserAdded(String name, String value) {
        if (lastUserAdded != null && name != null && value != null
                && ! "".equals(name) && ! "".equals(value)) {
            name = name.trim();
            lastUserAdded.addAttribute(name, value);
        }
    }
}