/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.backend.metadata.derby.internal;

import java.sql.Connection;

/**
 * A pooled Connection.
 * 
 * @author Christian Weiss
 */
interface PooledConnection extends Connection {

    void increment();

    void decrement();
}
