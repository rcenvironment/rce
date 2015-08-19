/*
 * Copyright (C) 2006-2010 DLR, Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.rce.validators.internal;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationServiceMessage;
import de.rcenvironment.core.configuration.ConfigurationServiceMessageEvent;

/**
 * Tests for {@link ConfigurationServiceParsingValidator}.
 * 
 * @author Christian Weiss
 */
public class ConfigurationServiceParsingValidatorTest {

    private final ConfigurationService configurationServiceMock = EasyMock
            .createMock(ConfigurationService.class);

    private final ConfigurationServiceMessage message = new ConfigurationServiceMessage(
            "message");
    
    private final ConfigurationServiceMessageEvent event = new ConfigurationServiceMessageEvent(configurationServiceMock, message);
    
    /** Test. */
    @Test
    public void testConstructor() {
        @SuppressWarnings({ "deprecation" })
        final ConfigurationServiceParsingValidator validator = new ConfigurationServiceParsingValidator();
        validator.activate(null);
    }

    /** Test. */
    @Test
    public void testHandleConfigurationServiceError() {
        @SuppressWarnings("deprecation")
        final ConfigurationServiceParsingValidator validator = new ConfigurationServiceParsingValidator();
        validator.handleConfigurationServiceError(event);
    }

    /** Test. */
    @Test
    public void testValidatePlatform() {
        @SuppressWarnings("deprecation")
        final ConfigurationServiceParsingValidator validator = new ConfigurationServiceParsingValidator();
        validator.clear();
        validator.handleConfigurationServiceError(event);
        Assert.assertEquals(1, validator.validatePlatform().size());
    }

}
