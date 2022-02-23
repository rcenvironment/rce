/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
