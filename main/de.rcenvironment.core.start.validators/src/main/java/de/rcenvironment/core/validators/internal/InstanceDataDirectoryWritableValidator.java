/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.validators.internal;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.start.common.validation.PlatformMessage;
import de.rcenvironment.core.start.common.validation.PlatformValidator;

/**
 * Validates RCE platform home directory. Check if it is writable.
 * 
 * @author Christian Weiss
 * 
 */
public class InstanceDataDirectoryWritableValidator implements PlatformValidator {

    private static ConfigurationService configurationService;

    private static CountDownLatch configurationServiceLatch = new CountDownLatch(1);

    @Deprecated
    public InstanceDataDirectoryWritableValidator() {
        // do nothing
    }

    protected void bindConfigurationService(final ConfigurationService newConfigurationService) {
        InstanceDataDirectoryWritableValidator.configurationService = newConfigurationService;
        configurationServiceLatch.countDown();
    }

    @Override
    public Collection<PlatformMessage> validatePlatform() {
        final Collection<PlatformMessage> result = new LinkedList<PlatformMessage>();
        File profileDir = null;
        final ConfigurationService boundConfigurationService = getConfigurationService();
        if (boundConfigurationService != null) {
            profileDir = boundConfigurationService.getProfileDirectory();
        } else {
            result.add(new PlatformMessage(PlatformMessage.Type.ERROR,
                ValidatorsBundleActivator.bundleSymbolicName,
                Messages.directoryNoConfigurationService));
        }
        if (profileDir == null) {
            result.add(new PlatformMessage(PlatformMessage.Type.ERROR,
                ValidatorsBundleActivator.bundleSymbolicName,
                Messages.directoryCouldNotBeRetrieved));
        } else {
            if (!profileDir.exists() || !profileDir.isDirectory()) {
                result.add(new PlatformMessage(PlatformMessage.Type.ERROR,
                    ValidatorsBundleActivator.bundleSymbolicName, Messages.directoryRceFolderDoesNotExist + profileDir));
            } else if (!profileDir.canRead() || !profileDir.canWrite()) {
                result.add(new PlatformMessage(PlatformMessage.Type.ERROR,
                    ValidatorsBundleActivator.bundleSymbolicName,
                    Messages.directoryRceFolderNotReadWriteAble + profileDir));
            }
        }
        return result;
    }

    /*
     * Please note: There is not unit test for this method, because too much OSGi API mocking would be needed for that little bit of code.
     * It is tested well, because it runs every time RCE starts up.
     */
    protected ConfigurationService getConfigurationService() {
        try {
            InstanceDataDirectoryWritableValidator.configurationServiceLatch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return InstanceDataDirectoryWritableValidator.configurationService;
    }

}
