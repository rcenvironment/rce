/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow.command;

import java.util.Optional;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A value class that holds the result of an endpoint exposure parameter given by the user when calling the command `wf integrate`. This
 * class represents parameters of the following forms:
 * 
 * <ul>
 * <li>`--expose componentId:endpoint`</li>
 * <li>`--expose componentId:internalEndpoint:externalEndpoint`</li>
 * <li>`--expose-input componentId:endpoint`</li>
 * <li>`--expose-input componentId:internalEndpoint:externalEndpoint`</li>
 * <li>`--expose-output componentId:endpoint`</li>
 * <li>`--expose-output componentId:internalEndpoint:externalEndpoint`</li>
 * </ul>
 * 
 * If the parameter starts with the flag `--expose`, we call this an ``endpoint exposure'', as it is unknown whether the user wants to
 * expose an input or an output by the given name. If there exist both an input and an output on the given name on the given component, the
 * integration will fail at a later point and ask the user to disambiguate. The notion of an ``endpoint exposure'' stands in contrast to an
 * ``input exposure'' and an ``output exposure'', which are determined by the user via the commands `--expose-input` and `--expose-output`,
 * respectively.
 * 
 * We furthermore distinguish between ``simple'' and ``renaming'' exposures. If the user requests a simple exposure, as in the first, third,
 * and fifth example above, the given endpoint will be exposed with the same name that it has internally. With a renaming exposure, which we
 * show in the second, fourth, and sixth example, the user wants the exposed endpoint to have some other name on the integrated component
 * than it originally has on the component in the integrated workflow.
 * 
 * This class does not represent integration parameters of the form `--expose componentId`, `--expose-inputs componentId`, and
 * `--expose-outputs componentId`. Such parameters are instead represented by the class {@link ComponentExposureParameter}.
 * 
 * @author Alexander Weinert
 */
final class EndpointExposureParameter {

    private final String componentId;

    private final String internalEndpointName;

    private final String externalEndpointName;

    private final Optional<Boolean> isInputExposure;

    private EndpointExposureParameter(String componentId, String internalEndpointName, String externalEndpointName,
        Optional<Boolean> isInputExposure) {
        this.componentId = componentId;
        this.internalEndpointName = internalEndpointName;
        this.externalEndpointName = externalEndpointName;
        this.isInputExposure = isInputExposure;
    }

    public static EndpointExposureParameter buildSimpleInputExposure(final String componentId, final String endpointName) {
        return new EndpointExposureParameter(componentId, endpointName, endpointName, Optional.of(true));
    }

    public static EndpointExposureParameter buildSimpleOutputExposure(final String componentId, final String endpointName) {
        return new EndpointExposureParameter(componentId, endpointName, endpointName, Optional.of(false));
    }

    public static EndpointExposureParameter buildSimpleEndpointExposure(final String componentId, final String endpointName) {
        return new EndpointExposureParameter(componentId, endpointName, endpointName, Optional.empty());
    }

    public static EndpointExposureParameter buildRenamingInputExposure(final String componentId, final String internalEndpointName,
        final String externalEndpointName) {
        return new EndpointExposureParameter(componentId, internalEndpointName, externalEndpointName, Optional.of(true));
    }

    public static EndpointExposureParameter buildRenamingOutputExposure(final String componentId, final String internalEndpointName,
        final String externalEndpointName) {
        return new EndpointExposureParameter(componentId, internalEndpointName, externalEndpointName, Optional.of(false));
    }

    public static EndpointExposureParameter buildRenamingEndpointExposure(final String componentId, final String internalEndpointName,
        final String externalEndpointName) {
        return new EndpointExposureParameter(componentId, internalEndpointName, externalEndpointName, Optional.empty());
    }

    public String getComponentId() {
        return this.componentId;
    }

    public String getInternalEndpointName() {
        return this.internalEndpointName;
    }

    public String getExternalEndpointName() {
        return this.externalEndpointName;
    }

    public boolean isInputExposure() {
        return this.isInputExposure.isPresent() && this.isInputExposure.get();
    }

    public boolean isOutputExposure() {
        return this.isInputExposure.isPresent() && !this.isInputExposure.get();
    }

    @Override
    public String toString() {
        final String endpointType;
        if (!this.isInputExposure.isPresent()) {
            endpointType = "endpointExposure";
        } else if (this.isInputExposure.get()) {
            endpointType = "inputExposure";
        } else {
            endpointType = "outputExposure";
        }
        return StringUtils.format("{%s,componentId=%s,internalEndpointName=%s,externalEndpointName=%s}", endpointType, componentId,
            internalEndpointName,
            externalEndpointName);
    }
}
