/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.script.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helperclass returns the defaultScript as String.
 * 
 * @author Tim Rosenbach
 */

public class DefaultScriptHelper {

	private static final Log LOGGER = LogFactory.getLog(DefaultScriptHelper.class);

	public static String getDefaultScript() {

		String returnValue = "";

		try {
			final InputStream is;
			if (DefaultScriptHelper.class.getResourceAsStream("/resources/defaultScript.py") == null) {
				is = new FileInputStream("./resources/defaultScript.py");
			} else {
				is = DefaultScriptHelper.class.getResourceAsStream("/resources/defaultScript.py");
			}
			returnValue = IOUtils.toString(is);
			IOUtils.closeQuietly(is);
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
		}
		return returnValue;

	}
}
