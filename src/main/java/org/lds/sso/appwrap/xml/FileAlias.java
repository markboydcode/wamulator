package org.lds.sso.appwrap.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.lds.sso.appwrap.XmlConfigLoader2;
import org.lds.sso.appwrap.exception.ConfigParserException;

public class FileAlias extends Alias {
	public static final String TAG_NAME = "file-alias";

	public FileAlias(String name, String value) {
		super(name, value);
	}
	
	public FileAlias(String name, String location, String defaultValue) {
		super(name, location, defaultValue);
	}
	
	@Override
	protected final String deriveFrom(String location) {
        File file = new File(location);
        try {
        	return StringUtils.trimToEmpty(FileUtils.readFileToString(file));
        } catch ( IOException e ) {
        	if ( e instanceof FileNotFoundException ) {
        		throw new ConfigParserException(
                        "File alias resource '" + location
                                + "' not found at '"
                                + file.getAbsolutePath() + "'.");
        	} else {
        		throw new ConfigParserException(
                    "Unable to load content for alias '"
                            + name
                            + "' from file resource '"
                            + file.getAbsolutePath() + "'.",
                    e);
        	}
        }
	}

	public String toString() {
		return XmlConfigLoader2.SRC_FILE + original;
	}
}
