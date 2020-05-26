/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.authorization.api;

import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Provides remote methods for requesting execution access to a specific shared component.
 *
 * @author Robert Mischke
 */
@RemotableService
public interface RemotableComponentExecutionAuthorizationService {

    /**
     * Requests an access token for a component that is made available to the "public" group by the instance receiving this call. As
     * everybody can call this method, and caller and callee share no known secret anyway, the generated token is returned in plain text.
     * <p>
     * On success, the generated token is registered internally to satisfy a single authorization check performed with
     * {@link ComponentExecutionAuthorizationService#verifyAndUnregisterExecutionToken()}.
     * 
     * @param internalComponentId the internal (technical) id of the requested component, without version suffix
     * @param componentVersion the version of the requested component
     * @return the generated token (in plain text form)
     * @throws RemoteOperationException on general remote method call errors
     * @throws OperationFailureException if acquiring the token failed, e.g. if the component is no longer public
     */
    String requestExecutionTokenForPublicComponent(String internalComponentId, String componentVersion)
        throws RemoteOperationException, OperationFailureException;

    /**
     * Requests an access token for a component that is made available to the specified access group by the instance receiving this call.
     * The generated token is encrypted with the specified group's secret key; the ability of the caller to decrypt it is taken as a simple
     * proof of group membership.
     * <p>
     * On success, the generated token is registered internally to satisfy a single authorization check performed with
     * {@link ComponentExecutionAuthorizationService#verifyAndUnregisterExecutionToken()}.
     * <p>
     * (Security design note: There is currently no real protection against a MitM-capable attacker intercepting and diverting this token
     * during workflow execution, so any additional security mechanism on this exchange would be pointless. To increase security, local
     * network network traffic should be point-to-point encrypted first. -- misc_ro, Feb 2019)
     * 
     * @param internalComponentId the internal (technical) id of the requested component, without version suffix
     * @param componentVersion the version of the requested component
     * @param groupId the public id of the group that the caller wants to use to gain access to this component; the method will check
     *        whether the component is actually authorized to be used by that group, and the caller will need access to the membership key
     *        of this group to decrypt the returned token
     * @return the generated token, encrypted with the group's secret key
     * @throws RemoteOperationException on general remote method call errors
     * @throws OperationFailureException if acquiring the token failed, e.g. if the component is no longer public
     */
    String requestEncryptedExecutionTokenViaGroupMembership(String internalComponentId, String componentVersion, String groupId)
        throws RemoteOperationException, OperationFailureException;
}
