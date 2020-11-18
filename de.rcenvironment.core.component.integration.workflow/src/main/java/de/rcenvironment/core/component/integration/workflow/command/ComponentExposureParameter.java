/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow.command;

/**
 * A value class that holds the result of a component exposure parameter given by the user when calling the command `wf integrate`. This
 * class represents parameters of the following forms:
 * 
 * <ul>
 * <li>`--expose <componentId>`</li>
 * <li>`--expose-inputs <componentId>`</li>
 * <li>`--expose-outputs <componentId>`</li>
 * </ul>
 * 
 * We call a parameter of the first kind an endpoint exposure, while we call parameters of the second and third kind input and output
 * exposures, respectively.
 * 
 * This class does not represent integration parameters of the form `--expose componentId:endpoint`, `--expose-input componentId:endpoint`,
 * or `--expose-output componentId:internalEndpoint:externalEndpoint`. Such parameters are instead represented by the class
 * {@link EndpointExposureParameter}.
 * 
 * @author Alexander Weinert
 */
final class ComponentExposureParameter {

    private final String componentId;

    private final boolean exposeInputs;

    private final boolean exposeOutputs;

    private ComponentExposureParameter(String componentId, boolean exposeInputs, boolean exposeOutputs) {
        this.componentId = componentId;
        this.exposeInputs = exposeInputs;
        this.exposeOutputs = exposeOutputs;
    }

    public static ComponentExposureParameter buildInputExposure(final String componentId) {
        return new ComponentExposureParameter(componentId, true, false);
    }

    public static ComponentExposureParameter buildOutputExposure(final String componentId) {
        return new ComponentExposureParameter(componentId, false, true);
    }

    public static ComponentExposureParameter buildEndpointExposure(final String componentId) {
        return new ComponentExposureParameter(componentId, true, true);
    }

    public String getComponentId() {
        return this.componentId;
    }

    public boolean shouldExposeInputs() {
        return this.exposeInputs;
    }

    public boolean shouldExposeOutputs() {
        return this.exposeOutputs;
    }

}
