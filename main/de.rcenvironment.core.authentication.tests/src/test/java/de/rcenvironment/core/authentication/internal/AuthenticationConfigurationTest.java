/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authentication.internal;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;
import de.rcenvironment.core.authentication.AuthenticationTestConstants;

/**
 * 
 * Test case for the class {@link AuthenticationConfiguration}.
 *
 * @author Doreen Seider
 */
public class AuthenticationConfigurationTest extends TestCase  {
    
    private List<String> caFiles = new ArrayList<String>();
  
    private List<String> crlFiles = new ArrayList<String>();

    private AuthenticationConfiguration myAuthenticationSettings = null;

    @Override
    public void setUp() throws Exception {
        myAuthenticationSettings = new AuthenticationConfiguration();
        
        caFiles.clear();
        caFiles.add(AuthenticationTestConstants.CA_FILE);
        caFiles.add(AuthenticationTestConstants.OTHER_CA_FILE);
        myAuthenticationSettings.setCaFiles(caFiles);
        
        crlFiles.clear();
        crlFiles.add(AuthenticationTestConstants.CRL_FILE);
        crlFiles.add(AuthenticationTestConstants.OTHER_CRL_FILE);
        myAuthenticationSettings.setCrlFiles(crlFiles);
    }
 
    /**
     * Test method for
     * {@link de.rcenvironment.core.authentication.internal.AuthenticationConfiguration#getCaFiles()}.
     */
    public void testGetCaFilesForSuccess() {
        caFiles.clear();
        caFiles.add(AuthenticationTestConstants.CA_FILE);
        caFiles.add(AuthenticationTestConstants.OTHER_CA_FILE);
        assertEquals(caFiles, myAuthenticationSettings.getCaFiles());
    }

    /**
     * Test method for
     * {@link de.rcenvironment.core.authentication.internal.AuthenticationConfiguration#getCrlFiles()}.
     */
    public void testGetCrlFilesForSuccess() {
        crlFiles.clear();
        crlFiles.add(AuthenticationTestConstants.CRL_FILE);
        crlFiles.add(AuthenticationTestConstants.OTHER_CRL_FILE);
        assertEquals(crlFiles, myAuthenticationSettings.getCrlFiles());
    }
    
    /**
     * Test method for
     * {@link de.rcenvironment.core.authentication.internal.AuthenticationConfiguration#getLdapServer()}.
     */
    public void testGetServerForSuccess() {
        String test = "newServer";
        myAuthenticationSettings.setLdapServer(test);
        Assert.assertTrue(myAuthenticationSettings.getLdapServer().equals(test));
    }
    
    /**
     * Test method for
     * {@link de.rcenvironment.core.authentication.internal.AuthenticationConfiguration#getLdapBaseDn()}.
     */
    public void testGetBaseDnForSuccess() {
        String test = "newBaseDn";
        myAuthenticationSettings.setLdapBaseDn(test);
        Assert.assertTrue(myAuthenticationSettings.getLdapBaseDn().equals(test));
    }
    
    /**
     * Test method for
     * {@link de.rcenvironment.core.authentication.internal.AuthenticationConfiguration#getLdapDomain()}.
     */
    public void testGetDomainForSuccess() {
        String test = "newDomain";
        myAuthenticationSettings.setLdapDomain(test);
        Assert.assertTrue(myAuthenticationSettings.getLdapDomain().equals(test));
    }
}
