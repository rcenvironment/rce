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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Test for {@link ValidatorsBundleActivator}.
 * 
 * @author Christian Weiss
 * 
 */
public class ValidatorsBundleActivatorTest {
    
    /** Test. 
     * @throws Exception */
    @Test
    public void testStartStop() throws Exception {
        final String symbolicName = "symbolicName";
        final ValidatorsBundleActivator activator = new ValidatorsBundleActivator();
        final BundleContext contextMock = EasyMock.createMock(BundleContext.class);
        final Bundle bundleMock = EasyMock.createMock(Bundle.class);
        EasyMock.expect(contextMock.getBundle()).andReturn(bundleMock);
        EasyMock.expect(bundleMock.getSymbolicName()).andReturn(symbolicName);
        EasyMock.replay(contextMock, bundleMock);
        activator.start(contextMock);
        Assert.assertEquals(new String(symbolicName), ValidatorsBundleActivator.bundleSymbolicName);
        activator.stop(contextMock);
    }

}
