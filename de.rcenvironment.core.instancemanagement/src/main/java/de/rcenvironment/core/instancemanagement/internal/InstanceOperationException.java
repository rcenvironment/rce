/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.instancemanagement.internal;

import java.io.IOException;

/**
 * 
 * {@link Exception} type for {@link InstanceOperationsImpl} failures.
 *
 * @author David Scholz
 */
public class InstanceOperationException extends IOException {
    
    private static final long serialVersionUID = 4901342547825941049L;
    
    public InstanceOperationException(String msg) {
        super(msg);
    }
     
}
