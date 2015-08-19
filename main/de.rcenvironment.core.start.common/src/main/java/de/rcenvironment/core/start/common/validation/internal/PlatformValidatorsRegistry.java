/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.common.validation.internal;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.core.start.common.validation.PlatformValidator;

/**
 * A manager class that manages the validation of the RCE platform thru the registered
 * {@link PlatformValidator}s.
 * 
 * @author Sascha Zur
 */
//TODO >5.0.0: rename - misc_ro
public class PlatformValidatorsRegistry {

    protected static final String VALIDATORS_ID = "de.rcenvironment.rce.start.validations";
    private static PlatformValidatorsRegistry instance = null;
    private static List<PlatformValidator> validators = new LinkedList<PlatformValidator>();
    
    /**
     * This is protected because of OSGi.
     */
    @Deprecated
    public PlatformValidatorsRegistry(){
        
    }
    
    /**
     * Get the instance of this class.
     * @return : instance
     */
    public static synchronized  PlatformValidatorsRegistry getDefaultInstance(){
        if (instance == null){
            instance =  new PlatformValidatorsRegistry();
        }
        return instance;
    }
    
    protected void bindPlatformValidators(final PlatformValidator newValidator) {
        validators.add(newValidator);  
    }

    protected void unbindPlatformValidators(final PlatformValidator oldValidator) {
        validators.remove(oldValidator);  
    }

    
    /**
     * Returns the registered {@link PlatformValidator}s.
     * 
     * @return the registered {@link PlatformValidator}s.
     */
    public List<PlatformValidator> getValidators() {
        return Collections.unmodifiableList(validators);
    }



}
