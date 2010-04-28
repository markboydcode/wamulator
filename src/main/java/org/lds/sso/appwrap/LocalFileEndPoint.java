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

	private String filePath = null;

	private String contentType = null;

	private boolean isRelative = false;

	public LocalFileEndPoint(String canonicalCtx, String filePathParam, String contentType) {
		this.canonicalContextRoot = canonicalCtx;
		this.filePath = filePathParam;
                
		if (this.filePath.endsWith("*")) {
			isRelative = true;
		}
		/*
		 * This handles the case of file
		 * <cctx-file cctx="/logs/*" 
         *      file="*" <<<< note only one char 
         *      content-type="text/plain"/>
		 */
		if (this.filePath.length() <= 2) {
			this.filePath = "";
		}
		else if (isRelative) {
			this.filePath = this.filePath.substring(0, this.filePath.length() - 1);
		}
		this.contentType = contentType;
		this.id = canonicalCtx + "->FILE=" + this.filePath;
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

	public String getFilePath() {
		return filePath;
	}

	public void setFilepath(String filePath) {
		this.filePath = filePath;
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
		if (isRelative){
			String uri = reqPkg.requestLine.getUri();
                        uri = uri.replace("../", ""); //Remove security problem with being able to go up directory hirarchies
			String relFilePath = uri.substring(canonicalContextRoot.length() );
			return filePath + relFilePath;
		}
		else {
			return filePath;
		}
	}
}
