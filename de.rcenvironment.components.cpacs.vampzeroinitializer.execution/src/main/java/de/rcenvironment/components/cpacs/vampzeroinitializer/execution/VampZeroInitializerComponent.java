/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.vampzeroinitializer.execution;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.cpacs.vampzeroinitializer.common.VampZeroInitializerComponentConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.api.Deprecated;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.component.xml.XmlComponentHistoryDataItem;
import de.rcenvironment.core.component.xml.api.EndpointXMLService;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * The "source" function based on aicraft predesign parameters.
 * 
 * @author Arne Bachmann
 * @author Markus Litz
 * @author Markus Kunde
 */

@Deprecated
public class VampZeroInitializerComponent extends DefaultComponent {

    private ComponentContext componentContext;

    private ComponentDataManagementService dataManagementService;

    /** XML Content from the configuration map (created by the view). */
    private String vampZeroInputs;

    private XmlComponentHistoryDataItem historyDataItem = null;

    private EndpointXMLService endpointXmlUtils;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public boolean treatStartAsComponentRun() {
        return componentContext.getInputs().isEmpty();
    }

    @Override
    public void start() throws ComponentException {
        dataManagementService = componentContext.getService(ComponentDataManagementService.class);
        endpointXmlUtils = componentContext.getService(EndpointXMLService.class);

        vampZeroInputs = componentContext.getConfigurationValue(VampZeroInitializerComponentConstants.XMLCONTENT);

        if (vampZeroInputs == null || vampZeroInputs.isEmpty()) {
            throw new ComponentException("No initial CPACS for VAMPzero given. Did you forget to click the 'Create CPACS' button?");
        }

        if (treatStartAsComponentRun()) {
            processInputs();
        }
    }

    @Override
    public void processInputs() throws ComponentException {
        initializeNewHistoryDataItem();
        File tempFile;
        try {
            tempFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("VAMPZeroInitializer-*.xml");
            FileUtils.writeStringToFile(tempFile, vampZeroInputs);
        } catch (IOException e) {
            throw new ComponentException("Failed to write initial CPACS into a temporary file "
                + "(that is required for VAMPzero Initializer)", e);
        }

        Map<String, TypedDatum> variableInputs = new HashMap<>();
        for (String inputName : componentContext.getInputsWithDatum()) {
            if (componentContext.isDynamicInput(inputName)) {
                variableInputs.put(inputName, componentContext.readInput(inputName));
            }
        }

        if (historyDataItem != null && !variableInputs.isEmpty()) {
            try {
                String plainVampZeroInputsFileRef = dataManagementService.createTaggedReferenceFromLocalFile(componentContext, tempFile,
                    VampZeroInitializerComponentConstants.CPACS_FILENAME);
                historyDataItem.setPlainXMLFileReference(plainVampZeroInputsFileRef);
                storeHistoryDataItem();
            } catch (IOException e) {
                String errorMessage = "Failed to store plain, initial CPACS file into the data management"
                    + "; it will not be available in the workflow data browser";
                String errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(
                    LogFactory.getLog(VampZeroInitializerComponent.class), errorMessage, e);
                componentContext.getLog().componentError(errorMessage, e, errorId);
            }
        }

        try {
            endpointXmlUtils.updateXMLWithInputs(tempFile, variableInputs, componentContext);
        } catch (DataTypeException e) {
            throw new ComponentException("Failed to add dynamic input values to the initial CPACS", e);
        }

        FileReferenceTD fileReference;
        try {
            fileReference = dataManagementService.createFileReferenceTDFromLocalFile(componentContext, tempFile,
                VampZeroInitializerComponentConstants.CPACS_FILENAME);
        } catch (IOException e) {
            throw new ComponentException("Failed to store initial CPACS file in the data management - "
                + "if it is not stored in the data management, it can not be sent as output value", e);
        }

        componentContext.writeOutput(VampZeroInitializerComponentConstants.OUTPUT_NAME_CPACS, fileReference);
        try {
            endpointXmlUtils.updateOutputsFromXML(tempFile, componentContext);
        } catch (DataTypeException e) {
            throw new ComponentException("Failed to extract dynamic output values from the initial CPACS", e);
        }
    }

    @Override
    public void completeStartOrProcessInputsAfterFailure() throws ComponentException {
        storeHistoryDataItem();
    }

    private void initializeNewHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            historyDataItem = new XmlComponentHistoryDataItem(VampZeroInitializerComponentConstants.COMPONENT_ID);
        }
    }

    private void storeHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            componentContext.writeFinalHistoryDataItem(historyDataItem);
        }
    }

}
