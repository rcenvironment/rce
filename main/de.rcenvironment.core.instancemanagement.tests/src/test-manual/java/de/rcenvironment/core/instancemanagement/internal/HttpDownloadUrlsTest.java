/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Test;

/**
 * Checks the availability of the HTTP download server.
 * 
 * @author Martin Misiak
 * @author Robert Mischke
 */
public class HttpDownloadUrlsTest {

    // TODO (p2) also check HTTPS and more specific download URLs - misc_ro

    // note: we are explicitly testing the HTTP URL here to ensure backwards compatibility after switching to HTTPS by default - misc_ro
    private static final String URL_ADDRESS = "http://software.dlr.de/updates/rce/8.x/";

    private static final int HTTP_CODE_OK = 200;

    private static final int EXPECTED_HTTP_CODE_FOR_REDIRECT_FROM_HTTP_URL = 302;

    private static final int MINUS_ONE = -1;

    /**
     * Sends a HTTP GET request to URL_ADDRESS. Fails if the response code is unexpected.
     */
    @Test
    public void testLegacyHttpUrl() {

        int httpCode = MINUS_ONE;

        try {
            URL url = new URL(URL_ADDRESS);
            HttpURLConnection connection;
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            httpCode = connection.getResponseCode();
        } catch (IOException e) {
            fail(e.getMessage());
        }

        assertEquals("Unexpected HTTP response code", EXPECTED_HTTP_CODE_FOR_REDIRECT_FROM_HTTP_URL, httpCode);
    }

}
