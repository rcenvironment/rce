/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.jetty.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.xml.ws.WebServiceException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit test for {@link JettyServiceImpl}.
 * 
 * @author Tobias Menden
 */
public class JettyServiceImplTest {

    private static final String ADDRESS = "http://localhost:6789/WebCallTest";

    private static final int REQUEST = 334;

    private static final int RESULT = 1000;

    private static JettyServiceImpl jettyService;

    /**
     * Common test setup.
     */
    @Before
    public void setUp() {
        jettyService = new JettyServiceImpl();
        WebCallImpl serverInstance = new WebCallImpl();
        jettyService.deployWebService(serverInstance, ADDRESS);
    }

    /**
     * Common test teardown.
     */
    @After
    public void tearDown() {
        jettyService.undeployWebService(ADDRESS);
    }

    /**
     * Test method for
     * 'de.rcenvironment.rce.communication.jetty.internal.JettyServiceImpl.undeployJetty()' for
     * success.
     */
    @Test
    public void testUndeployWebServiceForSuccess() {
        jettyService.undeployWebService(ADDRESS);
        WebCall testService = (WebCall) jettyService.createWebServiceClient(WebCall.class, ADDRESS);
        try {
            testService.call(REQUEST);
            fail();
        } catch (WebServiceException e) {
            assertNotNull(e);
        }
    }

    /**
     * Test method for
     * 'de.rcenvironment.rce.communication.jetty.internal.JettyServiceImpl.getWebServiceClient' for
     * sanity.
     */
    @Ignore("Sometimes fails with 'SocketException invoking http://localhost:6666/WebCallTest: Socket Closed'; "
        + "not critical as Jetty is not currently used in production")
    @Test
    public void testCreateWebServiceClientForSanity() {
        WebCall testService = (WebCall) jettyService.createWebServiceClient(WebCall.class, ADDRESS);
        assertEquals(RESULT, testService.call(REQUEST));
    }

    /**
     * Test method for 'de.rcenvironment.rce.communication.jetty.internal.WebCallImpl' for success.
     */
    @Test
    public void testCallForSucess() {
        WebCallImpl testCall = new WebCallImpl();
        int newResult = testCall.call(REQUEST);
        assertEquals(RESULT, newResult);
    }

}
