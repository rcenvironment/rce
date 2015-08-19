/*
 * Copyright (C) 2006-2010 DLR, Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.rce.validators.internal;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.configuration.ConfigurationService;


/**
 * Tests for {@link ConfigurationServiceParsingValidator}.
 * 
 * @author Christian Weiss
 */
public class DirectoryValidatorTest {
    
    /** Test. */
    @Test
    public void testExistingDirectory() {
        @SuppressWarnings("deprecation")
        final DirectoryValidator validator = new DirectoryValidator() {
            protected String getRceDirectoryPath() {
                final File tempFile = createTempFile();
                tempFile.mkdir();
                return tempFile.getAbsolutePath();
            };
        };
        Assert.assertEquals(0, validator.validatePlatform().size());
    }

    /** Test. */
    @Test
    public void testNullDirectory() {
        @SuppressWarnings("deprecation")
        final DirectoryValidator validator = new DirectoryValidator() {
            protected String getRceDirectoryPath() {
                return null;
            };
        };
        Assert.assertEquals(1, validator.validatePlatform().size());
    }

    /** Test. */
    @Test
    public void testNotExistingDirectory() {
        @SuppressWarnings("deprecation")
        final DirectoryValidator validator = new DirectoryValidator() {
            protected String getRceDirectoryPath() {
                final File tempFile = createTempFile();
                return tempFile.getAbsolutePath();
            };
        };
        Assert.assertEquals(1, validator.validatePlatform().size());
    }

    /** Test. */
    @Test
    public void testFile() {
        final AtomicReference<Exception> exception = new AtomicReference<Exception>();
        @SuppressWarnings("deprecation")
        final DirectoryValidator validator = new DirectoryValidator() {
            protected String getRceDirectoryPath() {
                final File tempFile = createTempFile();
                try {
                    tempFile.createNewFile();
                } catch (IOException e) {
                    exception.set(e);
                }
                return tempFile.getAbsolutePath();
            };
        };
        if (exception.get() != null) {
            throw new RuntimeException(exception.get());
        }
        Assert.assertEquals(1, validator.validatePlatform().size());
    }
    
    /** Test. */
    @Test
    public void testGetRceDirectoryPath() {
        // bind the ConfigurationService
        final DirectoryValidator validatorService = new DirectoryValidator();
        final ConfigurationService configurationServiceMock = EasyMock.createMock(ConfigurationService.class);
        validatorService.bindConfigurationService(configurationServiceMock);
        // test the DirectoryValidator
        final DirectoryValidator validator = new DirectoryValidator();
        validator.validatePlatform();
    }

    private File createTempFile() {
        File tempFile = null;
        while (tempFile == null || tempFile.exists()) {
            try {
                tempFile = File.createTempFile("DVAT-temp-file-" + UUID.randomUUID().toString(), "-tmp");
                if (tempFile.isFile()) {
                    tempFile.delete();
                }
            } catch (IOException e) {
                tempFile = null;
            }
        }
        return tempFile;
    }

}
