package org.lds.sso.appwrap;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class UriMatcher {

	
	public static boolean matches(String uri, String matchUri) {
		// case insensitive and replace the universal matcher {/.../*,*}
		// with !!! because the {} and * in the universal matcher is making
		// the logic more complex since the {} and * have individual meanings
		// outside of this context.
		if (uri == null || matchUri == null) {
			return false;
		}
		
		uri = uri.toLowerCase();
		matchUri = matchUri.toLowerCase().replace("{/.../*,*}", "!!!");
		
		boolean matches = false;
		
		if (uri.toLowerCase().equals(matchUri.toLowerCase())) {
			matches = true;
		} else if (matchUri.contains("{") && matchUri.contains("}")) {
			int start = matchUri.indexOf("{");
			int end = matchUri.indexOf("}");
			String theRest = matchUri.substring(end + 1);
			String theBeginning = matchUri.substring(0, start); 
			String theGuts = matchUri.substring(start + 1, end);
			String[] gutsParts = theGuts.split(",");
			for (String part : gutsParts) {
				String newMatchUri = theBeginning + part + theRest;
				if (!newMatchUri.startsWith("/")) {
					newMatchUri = "/" + newMatchUri;
				}
				matches = matches(uri, newMatchUri);
				if (matches) {
					break;
				}
				
			}
		} else if (matchUri.contains("?")) { 
			String[] parts = matchUri.split("/");
			int qIndex = 0;
			for (int i = 0; i < parts.length; i++) {
				if (parts[i].contains("?")) {
					qIndex = i;
					break;
				}
			}
			int qLoc = parts[qIndex].indexOf("?");
			parts[qIndex] = parts[qIndex].replace("?", "");
			
			String[] uriParts = uri.split("/");
			String part = uriParts[qIndex];
			if (part.length() > qLoc) {
				uriParts[qIndex] = part.substring(0, qLoc) + part.substring(qLoc + 1, part.length());
				matches = matches(StringUtils.join(uriParts, "/"), StringUtils.join(parts, "/"));
			}
		} else if (matchUri.contains("*")) {
			// Count star occurences to see if this has
			// multiple stars
			Pattern p = Pattern.compile("\\*");
			Matcher m = p.matcher(matchUri);
			int occurences = 0;
			while (m.find()) {
				occurences++;
			}
			
			if (matchUri.endsWith("*") && occurences <= 1) {
				if (uri.equals(matchUri.substring(0, matchUri.length() - 1))) {
					matches = true;
				} else {
					String[] parts = matchUri.split("/");
					int starIndex = 0;
					for (int i = 0; i < parts.length; i++) {
						if (parts[i].contains("*")) {
							starIndex = i;
							break;
						}
					}
					
					String part = parts[starIndex];
					if (part.length() == 1) {
						
						List<String> validParts = new ArrayList<String>();
						for (int i = 0; i < parts.length; i++) {
							if (i != starIndex) {
								validParts.add(parts[i]);
							}
						}
						String newMatchUri = StringUtils.join(validParts, "/");
						
						String[] uriParts = uri.split("/");
						if (uriParts.length >= parts.length) {
							validParts.clear();
							for (int i = 0; i < uriParts.length; i++) {
								if (i != starIndex) {
									validParts.add(uriParts[i]);
								}
							}
							String newUri = StringUtils.join(validParts, "/");
							
							matches = matches(newUri, newMatchUri);
						}
					}
				}
			} else {
				String[] parts = matchUri.split("/");
				int starIndex = 0;
				for (int i = 0; i < parts.length; i++) {
					if (parts[i].contains("*")) {
						starIndex = i;
						break;
					}
				}
				
				String part = parts[starIndex];
				if (part.length() > 1) {
					int loc = part.indexOf("*");
					String beforeStar = part.substring(0, loc);
					String afterStar = part.substring(loc + 1, part.length());
					
					String[] uriParts = uri.split("/");
					String uriPart = uriParts[starIndex];
					
					// find the matching part of the url
					int afterStarIndex = StringUtils.indexOfAny(afterStar, new char[] {'*', '?'});
					String afterStarPart = afterStarIndex > -1 ? afterStar.substring(0, afterStarIndex) : afterStar;
					if (uriPart.startsWith(beforeStar) && uriParts[starIndex].contains(afterStarPart) && 
							uriPart.length() >= beforeStar.length() + afterStarPart.length()) {
						
						// Strip off everything that comes before the star (assuming it matches)
						if (uriParts[starIndex].startsWith(beforeStar)) {
							uriParts[starIndex] = uriParts[starIndex].substring(beforeStar.length(), uriParts[starIndex].length());
							parts[starIndex] = parts[starIndex].substring(beforeStar.length(), parts[starIndex].length());
						} else {
							return false;
						}
						
						int matchIndex = uriParts[starIndex].indexOf(afterStarPart);
						uriParts[starIndex] = uriParts[starIndex].substring(matchIndex + afterStarPart.length(), uriParts[starIndex].length());
						String newUri = StringUtils.join(uriParts, "/");
						if (uri.endsWith("/") && !newUri.endsWith("/")) {
							newUri += "/";
						}
						
						matchIndex = parts[starIndex].indexOf(afterStarPart);
						parts[starIndex] = parts[starIndex].substring(matchIndex + afterStarPart.length(), parts[starIndex].length());
						String newMatchUri = StringUtils.join(parts, "/");
						
						matches = matches(newUri, newMatchUri);
					}
				} else {
					// Compare the two urls with the * portion of the url removed
					String newMatchUri = "";
					for (int i = 0; i < parts.length; i++) {
						if (i != starIndex && !parts[i].isEmpty()) {
							newMatchUri += "/" + parts[i];
						}
					}
					
					String newUri = "";
					String[] uriParts = uri.split("/");
					for (int i = 0; i < uriParts.length; i++) {
						if (i != starIndex && !uriParts[i].isEmpty()) {
							newUri += "/" + uriParts[i];
						}
					}
					if (uri.endsWith("/") && !newUri.endsWith("/")) {
						newUri += "/";
					}
					
					matches = matches(newUri, newMatchUri);
				}
			}
		} else if (matchUri.endsWith("!!!")) {
			String sub = matchUri.substring(0, matchUri.length() - 3);
			if (uri.toLowerCase().startsWith(sub.toLowerCase())) {
				matches = true;
			}
		}
		
		return matches;
	}
}
