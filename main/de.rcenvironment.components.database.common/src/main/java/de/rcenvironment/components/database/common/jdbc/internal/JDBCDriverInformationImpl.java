/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.database.common.jdbc.internal;

import de.rcenvironment.components.database.common.jdbc.JDBCDriverInformation;


/**
 * JDBC Driver Information data class.
 *
 * @author Oliver Seebach
 */
public class JDBCDriverInformationImpl implements JDBCDriverInformation {

    private final String urlScheme;
    
    private final String displayName;
    
    public JDBCDriverInformationImpl(String urlScheme, String displayName) {
        this.urlScheme = urlScheme;
        this.displayName = displayName;
    }

    @Override
    public String getUrlScheme() {
        return urlScheme;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

}
