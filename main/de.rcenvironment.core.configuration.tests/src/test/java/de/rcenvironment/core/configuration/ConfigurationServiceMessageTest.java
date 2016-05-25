/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration;

import java.util.Random;

import junit.framework.Assert;

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
