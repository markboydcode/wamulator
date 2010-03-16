package org.lds.sso.appwrap;

import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.RequestLine;
import org.lds.sso.appwrap.proxy.StartLine;

/**
 * Represents the mapping from canonical space URLs to a file on the local drive
 * for use in serving up resources that would, in the targeted SSO environment, be
 * served from remote servers. The idea is to allow for integration with other
 * applications while developing an application locally and yet not require that
 * those other applications be installed locally. If the resources acquired from
 * those other applications are large in quantity then this solution may be too
 * burdensome to use.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2010, The Church of Jesus Christ of Latter Day Saints
 * 
 */
public class LocalFileEndPoint implements EndPoint {
	private String canonicalContextRoot = null;

	private String applicationContextRoot = null;

	int endpointPort = -1;

	String host = null;

	private String id = null;

	private String filepath = null;

	private String contentType = null;

	public LocalFileEndPoint(String canonicalCtx, String filepath, String contentType) {
		this.canonicalContextRoot = canonicalCtx;
		this.filepath = filepath;
		this.contentType = contentType;
		this.id = canonicalCtx + "->FILE=" + filepath;
	}

	public String getId() {
		return this.id;
	}
	
	public int compareTo(EndPoint o) {
		return id.compareTo(o.getId());
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LocalFileEndPoint)) {
			return false;
		}
		LocalFileEndPoint ep = (LocalFileEndPoint) obj;
		return id.equals(ep.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	public String getCanonicalContextRoot() {
		return canonicalContextRoot;
	}

	public void setCanonicalContextRoot(String canonicalContextRoot) {
		this.canonicalContextRoot = canonicalContextRoot;
	}

	public String getFilepath() {
		return filepath;
	}

	public void setFilepath(String filepath) {
		this.filepath = filepath;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String type) {
		this.contentType = type;
	}

	/**
	 * TODO: add support for backward references and environment macros like the
	 * username of the current user or a user's header value. For now just return
	 * the filepath.
	 * 
	 * @param reqPkg
	 * @return
	 */
	public String getFilepathTranslated(HttpPackage reqPkg) {
		return filepath;
	}
}
