/*
 * Copyright 2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow;

import java.io.IOException;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapters;

public interface WorkflowIntegrationService {

    /**
     * This interface does not yet allow specification of a version of a group for the integrated component. If required, this may be
     * implemented later. Currently, the version is hardcoded to 0.0, while the group name is 'Workflows'. Similarly, there is currently no
     * option to set contact information for the integrator or documentation for the workflow.
     * 
     * @param workflow The workflow to integrate. Must not contain placeholders. This is not checked by the implementation, but causes
     *        problems when executing the integrated component.
     * @param componentname The name under which the component will be available in the group 'Workflows'.
     * @param endpointAdapterDefinitions A set of definitions which endpoints of the given workflow will be ``passed through'' as endpoints
     *        of the integrated component.
     * @throws IOException In order to integrate the workflow, this service writes some files to disk, namely into the directory
     *         `integration` of the current user profile. This exception is thrown if writing these files is not possible.
     */
    void integrateWorkflowFileAsComponent(WorkflowDescription workflow, String componentname,
        EndpointAdapters endpointAdapterDefinitions)
        throws IOException;

}
