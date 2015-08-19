/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.authentication.internal;

import java.io.InputStream;
import java.security.cert.X509Certificate;

import junit.framework.Assert;

import org.globus.gsi.CertUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.authentication.AuthenticationTestConstants;
import de.rcenvironment.core.authentication.CertificateUser;
import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.authentication.User.Type;


/**
 * Test cases for {@link CertificateUserTest}.
 *
 * @author Alice Zorn
 */
public class CertificateUserTest {

    private static final String CERTIFICATE_DN = "CN=RCE Admin,O=DLR,ST=Some-State,C=AU";

    private static final String CERTIFICATE_ID = "CN=Chief Engineer,O=DLR,L=Cologne,ST=Some-State,C=DE";

    /**
     * A proxy certificate for the tests.
     */
    private User myUser = null;

    /**
     * The underlying X509 certificate.
     */
    private X509Certificate myX509Certificate = null;
    
    private int validityInDays = 7;

    /**
     * Set up test.
     * 
     * @throws Exception if an error occurs.
     */
    @Before
    public void setUp() throws Exception {
        InputStream inputStream = getClass().getResourceAsStream(AuthenticationTestConstants.USERCERT_RCE_ENGINEER_PEM);
        myX509Certificate = CertUtil.loadCertificate(inputStream);
        myUser = new CertificateUser(myX509Certificate, validityInDays);
    }

    /**
     * Tear down test.
     * 
     * @throws Exception if an error occurs.
     */
    @After
    public void tearDown() throws Exception {
        myUser = null;
        myX509Certificate = null;
    }

    /**
     * Test if the user id can be retrieved from a certificate user.
     */
    @Test
    public void testGetUserIDForSuccess() {
        Assert.assertEquals(CERTIFICATE_ID, myUser.getUserId());
    }
    
    /**
     * Test if the domain can be retrieved from a certificate user.
     */
    @Test
    public void testGetDomainForSuccess() {
        Assert.assertEquals(CERTIFICATE_DN, myUser.getDomain());
    }
    
    /**
     * Test if the type is correct.
     */
    @Test
    public void testGetTypeForSuccess(){
        Assert.assertEquals(Type.certificate, myUser.getType());
    }
 
}
