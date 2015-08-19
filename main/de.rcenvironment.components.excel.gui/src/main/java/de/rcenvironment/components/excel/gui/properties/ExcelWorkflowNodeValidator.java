/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.gui.properties;


import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.components.excel.common.ExcelComponentConstants;
import de.rcenvironment.components.excel.common.ExcelException;
import de.rcenvironment.components.excel.common.ExcelService;
import de.rcenvironment.components.excel.common.ExcelUtils;
import de.rcenvironment.components.excel.common.SimpleExcelService;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;
import de.rcenvironment.rce.components.excel.commons.ExcelAddress;


/**
 * A {@link AbstractWorkflowNodeValidator} implementation to validate the GUI of the Excel
 * component.
 * 
 * <p>
 * To be incorporated in the validation process, this validator is registered as an extension to the
 * extension point "de.rcenvironment.core.gui.workflow.nodeValidators" in the plugin.xml.
 * </p>
 *
 * @author Markus Kunde
 */
public class ExcelWorkflowNodeValidator extends AbstractWorkflowNodeValidator {
    
    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {
        ExcelService excelService = new SimpleExcelService();
        
        final List<WorkflowNodeValidationMessage> messages = new LinkedList<WorkflowNodeValidationMessage>();
        
        //Get all relevant validation items
        String excelFile = getProperty(ExcelComponentConstants.XL_FILENAME);
        boolean driver = Boolean.valueOf(getProperty(ExcelComponentConstants.DRIVER));   

        //Check
        File xlFile = ExcelUtils.getAbsoluteFile(excelFile);
        
        if (xlFile == null || !xlFile.exists() || !excelService.isValidExcelFile(xlFile)) {
            final WorkflowNodeValidationMessage validationMessage = 
                new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.ERROR, 
                    ExcelComponentConstants.XL_FILENAME,
                    Messages.errorNoExcelFileRelative, 
                    Messages.bind(Messages.errorNoExcelFileRelative, null));
            
            messages.add(validationMessage);
        }

        if (driver && getOutputs().isEmpty()) {
            final WorkflowNodeValidationMessage validationMessage =
                new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.WARNING,
                    ExcelComponentConstants.DRIVER,
                    Messages.errorNoOutputAsDriverRelative,
                    Messages.bind(Messages.errorNoOutputAsDriverAbsolute,
                        ExcelComponentConstants.DRIVER));
            messages.add(validationMessage);
        }
        
        //TODO Check of endpoints runs to long for execution in GUI-thread. 
        //Validation should be called in another thread on a higher level        
        /*for (final EndpointDescription inputName: getInputs()) {
            messages.addAll(testChannelMetaData(xlFile, inputName));            
        }
        
        for (final EndpointDescription outputName: getOutputs()) {
            messages.addAll(testChannelMetaData(xlFile, outputName));
        }*/
        
        return messages;
    }
    
    
    private List<WorkflowNodeValidationMessage> testChannelMetaData(final File xlFile, EndpointDescription endpointDesc) {
        final List<WorkflowNodeValidationMessage> messages = new LinkedList<WorkflowNodeValidationMessage>();
        
        String address = endpointDesc.getMetaDataValue(ExcelComponentConstants.METADATA_ADDRESS);
        final WorkflowNodeValidationMessage validationMessage =
            new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.ERROR,
                ExcelComponentConstants.METADATA_ADDRESS,
                Messages.errorNoMetaDataAddressRelative,
                Messages.bind(Messages.errorNoMetaDataAddressAbsolute,
                    ExcelComponentConstants.METADATA_ADDRESS));
        try {
            new ExcelAddress(xlFile, address);
        } catch (ExcelException e) {
            messages.add(validationMessage);
        }
        
        return messages;
    }

}
