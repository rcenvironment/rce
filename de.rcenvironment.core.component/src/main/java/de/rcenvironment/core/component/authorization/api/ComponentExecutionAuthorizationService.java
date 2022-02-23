/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.authorization.api;

/**
 * Generates and manages access tokens that authorize a single execution of a specific component.
 *
 * @author Robert Mischke
 */
public interface ComponentExecutionAuthorizationService {

    /**
     * Creates and registers an access token for a component on the node where a workflow is initiated from. This allows remote workflow
     * controllers to access components on the workflow-initiating machine if those components are not otherwise published.
     * 
     * @param compIdAndVersion the instance-wide unique component id
     * @return the generated token
     */
    String createAndRegisterExecutionTokenForLocalComponent(String compIdAndVersion);

    /**
     * Verifies whether the given token was previously registered, and has not expired yet. If that token is found, it is unregistered and
     * cannot be used again.
     * 
     * @param token the token to check
     * @return true if the token was present and still valid
     */
    boolean verifyAndUnregisterExecutionToken(String token);
}
