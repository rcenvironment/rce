/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.sql.common;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;


/**
 * Service interface for a JDBC provider.
 *
 * @author Christian Weiss
 */
public interface JDBCService {
    
    /**
     * Returns the configuration {@link JDBCProfileImpl}s.
     * 
     * @return the {@link JDBCProfileImpl}s
     */
    List<JDBCProfile> getProfiles();
    
    /**
     * Returns the {@link JDBCProfileImpl} with the given label.
     * 
     * @param label the label of the desired {@link JDBCProfileImpl}
     * @return the {@link JDBCProfileImpl}
     */
    JDBCProfile getProfileByLabel(String label);

    /**
     * Returns a {@link Connection} instance for the given {@link JDBCProfileImpl}.
     * 
     * @param profile the {@link JDBCProfileImpl}
     * @return a {@link Connection} for given {@link JDBCProfileImpl}
     * @throws SQLException in case an {@link SQLException} occurs
     */
    Connection getConnection(JDBCProfile profile) throws SQLException;

}
