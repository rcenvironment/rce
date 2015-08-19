/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.impl.NetworkContactPointImpl;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Utility methods related to {@link NetworkContactPoint}s.
 * 
 * @author Robert Mischke
 */
public final class NetworkContactPointUtils {

    private static final Pattern NCP_DEFINITION_PATTERN = Pattern.compile("^\\s*([\\w\\-]+):([\\w.\\-]+):(\\d+)(?:\\s*\\((.*)\\))?\\s*$");

    private static final Pattern ATTRIBUTE_ENTRY_PATTERN = Pattern.compile("^([^=]+)=(.*)$");

    private NetworkContactPointUtils() {}

    /**
     * Converts the (transport-specific) String representation of a {@link NetworkContactPoint} into its object representation.
     * 
     * @param contactPointDef the String representation
     * @return the object representation
     * @throws IllegalArgumentException if the String is malformed
     */
    public static NetworkContactPoint parseStringRepresentation(String contactPointDef) throws IllegalArgumentException {
        Matcher m = NCP_DEFINITION_PATTERN.matcher(contactPointDef);
        if (!m.matches()) {
            throw new IllegalArgumentException();
        }
        String host = m.group(2);
        int port = Integer.parseInt(m.group(3));
        String transportId = m.group(1);
        NetworkContactPointImpl ncp = new NetworkContactPointImpl(host, port, transportId);
        String attributePart = m.group(4);
        if (attributePart != null) {
            Map<String, String> newAttributes = parseAttributePart(attributePart);
            if (!newAttributes.isEmpty()) {
                ncp.setAttributes(newAttributes);
            }
        }
        return ncp;
    }

    private static Map<String, String> parseAttributePart(String attributePart) {
        Map<String, String> newAttributes = new HashMap<String, String>();
        String[] attributeEntries = attributePart.split(",");
        for (String entry : attributeEntries) {
            String trimmedEntry = entry.trim();
            if (trimmedEntry.isEmpty()) {
                continue;
            }
            Matcher attrMatcher = ATTRIBUTE_ENTRY_PATTERN.matcher(trimmedEntry);
            if (!attrMatcher.matches()) {
                LogFactory.getLog(NetworkContactPointUtils.class).warn("Invalid attribute entry: \"" + entry + "\"");
                continue;
            }
            String key = attrMatcher.group(1).trim();
            String val = attrMatcher.group(2);
            if (val != null) {
                val = val.trim();
            } else {
                val = "";
            }
            newAttributes.put(key, val);
        }
        return newAttributes;
    }

    /**
     * Restores the original string representation from a {@link NetworkContactPoint}.
     * 
     * @param ncp the NCP
     * @return the string representation
     */
    public static String toDefinitionString(NetworkContactPoint ncp) {
        String attributesSuffix = "";
        Map<String, String> attributes = ncp.getAttributes();
        if (attributes.size() != 0) {
            // note: quick&dirty formatting; improve as necessary
            attributesSuffix = attributes.toString();
            attributesSuffix = "(" + attributesSuffix.substring(1, attributesSuffix.length() - 1) + ")";
        }
        // note: intentionally using %s for the port to avoid locale-based formatting
        return StringUtils.format("%s:%s:%s%s", ncp.getTransportId(), ncp.getHost(), ncp.getPort(), attributesSuffix);
    }

}
