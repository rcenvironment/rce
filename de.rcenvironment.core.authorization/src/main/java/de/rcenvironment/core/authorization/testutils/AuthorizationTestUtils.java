/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization.testutils;

import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.authorization.cryptography.internal.BCCryptographyOperationsProviderImpl;
import de.rcenvironment.core.authorization.internal.AuthorizationServiceImpl;

/**
 * Test utilities for authorization-related classes.
 *
 * @author Robert Mischke
 */
public final class AuthorizationTestUtils {

    private AuthorizationTestUtils() {}

    /**
     * @return a default {@link AuthorizationService} implementation.
     */
    public static AuthorizationService createAuthorizationServiceStub() {
        final AuthorizationServiceImpl authorizationService = new AuthorizationServiceImpl();
        authorizationService.bindCryptographyOperationsProvider(new BCCryptographyOperationsProviderImpl());
        return authorizationService;
    }
}
