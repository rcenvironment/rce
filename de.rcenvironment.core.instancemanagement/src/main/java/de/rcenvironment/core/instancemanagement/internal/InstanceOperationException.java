/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
