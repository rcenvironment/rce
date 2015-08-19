/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.sql.common.internal;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.sql.common.JDBCProfile;
import de.rcenvironment.core.configuration.ConfigurationSegment;

/**
 * JDBC configuration.
 * 
 * @author Christian Weiss
 * @author Robert Mischke
 */
public class JDBCConfiguration {

    private List<JDBCProfile> profiles = new LinkedList<JDBCProfile>();

    public JDBCConfiguration(ConfigurationSegment configurationSegment) {
        Log log = LogFactory.getLog(getClass());
        Map<String, ConfigurationSegment> elements = configurationSegment.listElements("profiles");
        for (Entry<String, ConfigurationSegment> entry : elements.entrySet()) {
            try {
                profiles.add(entry.getValue().mapToObject(JDBCProfile.class));
            } catch (IOException e) {
                log.error("Error parsing JDBC profile " + entry.getKey());
            }
        }
        log.debug("Loaded JDBC profiles: " + profiles);
    }

    /**
     * Returns the list of {@link JDBCProfile} contained in this {@link JDBCConfiguration}.
     * 
     * @return the list of {@link JDBCProfile}
     */
    public List<JDBCProfile> getProfiles() {
        return profiles;
    }

}
