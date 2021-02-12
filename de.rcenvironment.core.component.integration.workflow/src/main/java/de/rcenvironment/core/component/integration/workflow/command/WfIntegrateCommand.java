/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow.command;

import java.util.Collection;

final class WfIntegrateCommand {
    
    private String componentId;
    
    private String workflowfilePath;
    
    private Collection<EndpointExposureParameter> endpointExposures;
    
    private Collection<ComponentExposureParameter> componentExposures;
    
    static class Builder {
        private WfIntegrateCommand product = new WfIntegrateCommand();
        
        public Builder setComponentId(String componentId) {
            product.componentId = componentId;
            return this;
        }
        
        public Builder setWorkflowfilePath(String workflowfilePath) {
            product.workflowfilePath = workflowfilePath;
            return this;
        }
        
        public Builder addEndpointExposure(EndpointExposureParameter endpointExposure) {
            product.endpointExposures.add(endpointExposure);
            return this;
        }
        
        public Builder addComponentExposure(ComponentExposureParameter componentExposure) {
            product.componentExposures.add(componentExposure);
            return this;
        }
        
        public WfIntegrateCommand build() {
            final WfIntegrateCommand returnValue = product;
            this.product = null;
            return returnValue;
        }
    }
    
    // We make the constructor private in order to enforce use of the Builder class
    private WfIntegrateCommand() {}

    
    String getComponentId() {
        return componentId;
    }

    
    String getWorkflowfilePath() {
        return workflowfilePath;
    }

    
    Collection<EndpointExposureParameter> getEndpointExposures() {
        return endpointExposures;
    }

    
    Collection<ComponentExposureParameter> getComponentExposures() {
        return componentExposures;
    }
}
