/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.internal;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDatumImpl;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * Writes {@link TypedDatum}s to outputs.
 *
 * @author Doreen Seider
 */
public class TypedDatumToOutputWriter {
    
    private static final Log LOG = LogFactory.getLog(TypedDatumToOutputWriter.class);

    private static final boolean VERBOSE_LOGGING = DebugSettings.getVerboseLoggingEnabled(TypedDatumToOutputWriter.class);
    
    private static EndpointDatumDispatcher endpointDatumDispatcher;
    
    private ComponentExecutionContext compExeCtx;
    
    @Deprecated
    public TypedDatumToOutputWriter() {}
    
    protected TypedDatumToOutputWriter(ComponentExecutionRelatedInstances compExeRelatedInstances) {
        this.compExeCtx = compExeRelatedInstances.compExeCtx;
    }

    protected void writeTypedDatumToOutput(String outputName, TypedDatum datumToSend) {
        writeTypedDatumToOutput(outputName, datumToSend, null);
    }

    protected void writeTypedDatumToOutput(String outputName, TypedDatum datumToSend, Long outputDmId) {
        writeTypedDatumToCertainConnectedOutputs(outputName, datumToSend, null, null, outputDmId);
    }
    
    protected void writeTypedDatumToOutputConsideringOnlyCertainInputs(String outputName, TypedDatum datumToSend, String inputCompExeId,
        String inputName) {
        writeTypedDatumToCertainConnectedOutputs(outputName, datumToSend, inputCompExeId, inputName, null);
    }

    private void writeTypedDatumToCertainConnectedOutputs(String outputName, TypedDatum datumToSend, String inputCompExeId,
        String inputName, Long outputDmId) {
        Map<String, List<EndpointDatumRecipient>> endpointDatumRecipients = compExeCtx.getEndpointDatumRecipients();
        if (endpointDatumRecipients.containsKey(outputName)) {
            for (EndpointDatumRecipient epRecipient : endpointDatumRecipients.get(outputName)) {
                if (considerRecipient(epRecipient, inputCompExeId, inputName)) {
                    EndpointDatumImpl endpointDatum = new EndpointDatumImpl();
                    endpointDatum.setEndpointDatumRecipient(epRecipient);
                    endpointDatum.setOutputsComponentExecutionIdentifier(
                        compExeCtx.getExecutionIdentifier());
                    endpointDatum.setOutputsNodeId(compExeCtx.getNodeId());
                    endpointDatum.setWorkflowExecutionIdentifier(
                        compExeCtx.getWorkflowExecutionIdentifier());
                    endpointDatum.setWorkflowNodeId(compExeCtx.getWorkflowNodeId());
                    endpointDatum.setDataManagementId(outputDmId);
                    endpointDatum.setValue(datumToSend);
                    endpointDatumDispatcher.dispatchEndpointDatum(endpointDatum);

                    if (VERBOSE_LOGGING) {
                        LOG.debug(StringUtils.format("Sent at %s@%s: %s (-> %s@%s)", outputName,
                            compExeCtx.getInstanceName(), datumToSend,
                            epRecipient.getInputName(), epRecipient.getInputsComponentInstanceName()));
                    }
                }
            }
        }
    }
    
    private boolean considerRecipient(EndpointDatumRecipient epRecipient, String inputCompExeId, String inputName) {
        return inputCompExeId == null
            || (epRecipient.getInputName().equals(inputName) && epRecipient.getInputsComponentExecutionIdentifier().equals(inputCompExeId));
    }
    
    protected void bindEndpointDatumDispatcher(EndpointDatumDispatcher newService) {
        TypedDatumToOutputWriter.endpointDatumDispatcher = newService;
    }
}
