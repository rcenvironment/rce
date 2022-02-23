/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration;

import java.util.Random;

import org.junit.Assert;

import org.junit.Test;

/**
 * Test case for {@link ConfigurationServiceMessage}.
 * 
 * @author Christian Weiss
 */
public class ConfigurationServiceMessageTest {

    /** Test. */
    @Test
    public void testGetMessage() {
        final String text = "testText " + (new Random()).nextLong();
        final ConfigurationServiceMessage message = new ConfigurationServiceMessage(text);
        Assert.assertEquals(new String(text), message.getMessage());
    }

}
