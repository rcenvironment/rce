/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test case for {@link ConfigurationServiceMessageEvent}.
 * 
 * @author Christian Weiss
 */
public class ConfigurationServiceMessageEventTest {

    private final ConfigurationService configurationServiceMock = EasyMock.createMock(ConfigurationService.class);

    private final ConfigurationServiceMessage message = new ConfigurationServiceMessage("message");

    /** Test. */
    @Test
    public void testConstructorForSuccess() {
        @SuppressWarnings("unused")
        final ConfigurationServiceMessageEvent event = new ConfigurationServiceMessageEvent(configurationServiceMock, message);
    }

    /** Test. */
    @Test
    public void testConstructorForFailure() {
        try {
            @SuppressWarnings("unused")
            final ConfigurationServiceMessageEvent event = new ConfigurationServiceMessageEvent(null, null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            e = null;
        }
        try {
            @SuppressWarnings("unused")
            final ConfigurationServiceMessageEvent event = new ConfigurationServiceMessageEvent(configurationServiceMock, null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            e = null;
        }
    }

    /** Test. */
    @Test
    public void testGetSource() {
        final ConfigurationServiceMessageEvent event = new ConfigurationServiceMessageEvent(configurationServiceMock, message);
        Assert.assertNotNull(event.getSource());
    }

    /** Test. */
    @Test
    public void testGetError() {
        final ConfigurationServiceMessageEvent event = new ConfigurationServiceMessageEvent(configurationServiceMock, message);
        Assert.assertNotNull(event.getError());
    }

}
