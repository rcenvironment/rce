/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.login;

import junit.framework.TestCase;
import de.rcenvironment.core.authentication.AuthenticationException;
import de.rcenvironment.core.login.internal.ServiceHandler;

/**
 * Test cases for {@link AbstractLogin}.
 *
 * @author Doreen Seider
 * @author Tobias Menden
 */
public class AbstractLoginTest extends TestCase {
    
    /** Inherited class of the abstract class under test. */
    private LoginNoInput loginNoInput;
    
    /**Inherited class of the abstract class under test. */
    private LoginWithCertificateInput loginWithCertificateInput;
    
    /**Inherited class of the abstract class under test. */
    private LoginWithLdapInput loginWithLdapInput;
    
    /** Inherited class of the abstract class under test. */
    private LoginWithException loginWithException;
            
    @Override
    public void setUp() throws Exception {
        ServiceHandler serviceHandler = new ServiceHandler();
        serviceHandler.bindConfigurationService(LoginMockFactory.getInstance().getConfigurationServiceMock());
        serviceHandler.bindAuthenticationService(LoginMockFactory.getInstance().getAuthenticationServiceMock());
        serviceHandler.bindNotificationService(LoginMockFactory.getInstance().getNotificationServiceMock());
        serviceHandler.activate(LoginMockFactory.getInstance().getBundleContextMock());
        loginNoInput = new LoginNoInput();
        loginWithCertificateInput = new LoginWithCertificateInput();
        loginWithLdapInput = new LoginWithLdapInput();
        loginWithException = new LoginWithException();
    }
    
    /**
     * Test.
     */
    public void testLogin() {
        loginNoInput.login();
        loginWithException.login();
        loginWithCertificateInput.login();
        
    }
    
    /**
     * Test.
     */
    public void testLoginLdap() {
        loginWithLdapInput.setLoginInput(new LoginInput("a", "b"));
        loginWithLdapInput.login();

//        loginWithLdapInput.setLoginInput(new LoginInput("a", "c"));
//        loginWithLdapInput.login();
    }
    
    /**
     * Test.
     */
    public void testLogout() {
        loginNoInput.logout();
    }
    
    
    /**
     * Test.
     */
    public void testCreateLoginInputLdap(){
        try {
            LoginInput input = loginWithLdapInput.createLoginInputLDAP("f_rcelda", "test987!");
        } catch (AuthenticationException e) {
//            fail();
            e = null;
        }
    }

}

/**
 * Sub class of the abstract class under test.
 * 
 * @author Doreen Seider
 */
class LoginNoInput extends AbstractLogin {

    @Override
    protected LoginInput getLoginInput() throws AuthenticationException {
        return null;
    }

    @Override
    protected void informUserAboutError(String errorMessage, Throwable e) {
    }
    
}

/**
 * Sub class of the abstract class under test.
 * 
 * @author Doreen Seider
 */
class LoginWithCertificateInput extends AbstractLogin {
    
    @Override
    protected LoginInput getLoginInput() throws AuthenticationException {
        return LoginInputFactory.getLoginInputForCertificate();
    }

    @Override
    protected void informUserAboutError(String errorMessage, Throwable e) {
    }
    
}

/**
 * Sub class of the abstract class under test.
 * 
 * @author Alice Zorn
 */
class LoginWithLdapInput extends AbstractLogin {
    
    private LoginInput loginInput;
    
    public void setLoginInput(final LoginInput loginInput) {
        this.loginInput = loginInput;
    }
    
    @Override
    protected LoginInput getLoginInput() throws AuthenticationException {
        return loginInput;
    }

    @Override
    protected void informUserAboutError(String errorMessage, Throwable e) {
    }
    
}

/**
 * Sub class of the abstract class under test.
 * 
 * @author Doreen Seider
 */
class LoginWithException extends AbstractLogin {
    
    @Override
    protected LoginInput getLoginInput() throws AuthenticationException {
        throw new AuthenticationException("");
    }

    @Override
    protected void informUserAboutError(String errorMessage, Throwable e) {
    }
    
}
