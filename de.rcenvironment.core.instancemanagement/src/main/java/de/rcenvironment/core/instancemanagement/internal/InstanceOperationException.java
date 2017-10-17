/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.instancemanagement.internal;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 
 * Wraps {@link IOException} to save failed instances which have caused that exception.
 *
 * @author David Scholz
 */
public class InstanceOperationException extends IOException {
    
    private static final long serialVersionUID = 4901342547825941049L;
    
    private final List<File> failedInstances;
    
    private final boolean isListPresent;
    
    public InstanceOperationException(final List<File> failedInstances) {
        super();
        if (failedInstances == null) {
            this.isListPresent = false;
        } else {
            this.isListPresent = true;
        }
        this.failedInstances = failedInstances;
    }
    
    public InstanceOperationException(String msg, final List<File> failedInstances) {
        super(msg);
        if (failedInstances == null) {
            this.isListPresent = false;
        } else {
            this.isListPresent = true;
        }
        this.failedInstances = failedInstances;
    }
    
    public InstanceOperationException(String msg, Exception e, final List<File> failedInstances) {
        super(msg, e);
        if (failedInstances == null) {
            this.isListPresent = false;
        } else {
            this.isListPresent = true;
        }
        this.failedInstances = failedInstances;
    }
    
    public InstanceOperationException(Exception e, final List<File> failedInstances) {
        super(e);
        if (failedInstances == null) {
            this.isListPresent = false;
        } else {
            this.isListPresent = true;
        }
        this.failedInstances = failedInstances;
    }
    
    /**
     * 
     * Get failed instances causing this exception.
     * 
     * @return failed instances.
     */
    public List<File> getFailedInstances() {
        if (isListPresent) {
            return failedInstances;
        } else {
            // easier to handle than null or throwing {@link IOException}.
            return Collections.emptyList();
        }
    }
    
}
