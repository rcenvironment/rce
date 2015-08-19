/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.validators.internal;

import java.util.Collection;
import java.util.LinkedList;

import de.rcenvironment.core.start.common.validation.PlatformMessage;
import de.rcenvironment.core.start.common.validation.PlatformValidator;

/**
 * Validator to ensure the length of the rce path is not too long for the filesystem.
 * @author Sascha Zur
 * 
 *
 */
public class PathlengthValidator implements PlatformValidator{
    
    private static final int MAX_LENGTH_WINDOWS_7_UNIX = 255;
    
    private static final int MAX_LENGTH_PLUGIN_NAMES = 96; //Optimizer
        
    @Override
    public Collection<PlatformMessage> validatePlatform() {
        final Collection<PlatformMessage> result = new LinkedList<PlatformMessage>();
        final int maximumPathLength = getMaximumLengthForOS();
        String rceInstallationPath =  System.getProperty("eclipse.home.location");
       
        if (rceInstallationPath.length() + MAX_LENGTH_PLUGIN_NAMES > maximumPathLength){
            result.add(new PlatformMessage(PlatformMessage.Type.ERROR,
                     ValidatorsBundleActivator.bundleSymbolicName,
                     Messages.directoryRceFolderPathTooLong + " : \n" + rceInstallationPath));
        }
         
        return result;
    }
    private int getMaximumLengthForOS() {
        int result = 0 - 1;
        
        result = MAX_LENGTH_WINDOWS_7_UNIX;
         
        return result;
    }
    
}
