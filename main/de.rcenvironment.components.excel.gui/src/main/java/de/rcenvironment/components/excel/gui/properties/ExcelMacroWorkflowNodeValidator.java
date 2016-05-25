/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.gui.properties;


import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.excel.common.ExcelComponentConstants;
import de.rcenvironment.components.excel.common.ExcelException;
import de.rcenvironment.components.excel.common.ExcelService;
import de.rcenvironment.components.excel.common.ExcelUtils;
import de.rcenvironment.components.excel.common.SimpleExcelService;
import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;


/**
 * A {@link AbstractWorkflowNodeValidator} implementation to validate the GUI of the Excel
 * component.
 * 
 * This implementation is special for VBA Macros because unstable OLE implementation should be used as least as possible
 * and this validation can only be used in MS Windows and MS Office environments. So therefore this class can be moved later 
 * to a special Windows bundle.
 * 
 * <p>
 * To be incorporated in the validation process, this validator is registered as an extension to the
 * extension point "de.rcenvironment.core.gui.workflow.nodeValidators" in the plugin.xml.
 * </p>
 *
 * @author Markus Kunde
 */
public class ExcelMacroWorkflowNodeValidator extends AbstractWorkflowNodeValidator {
        
    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {
        ExcelService excelService = new SimpleExcelService();
        
        final List<WorkflowNodeValidationMessage> messages = new LinkedList<WorkflowNodeValidationMessage>();
        
        //Get all relevant validation items
        String excelFile = getProperty(ExcelComponentConstants.XL_FILENAME);
        String premacro = getProperty(ExcelComponentConstants.PRE_MACRO);
        String runmacro = getProperty(ExcelComponentConstants.RUN_MACRO);
        String postmacro = getProperty(ExcelComponentConstants.POST_MACRO);

        //Check
        File xlFile = ExcelUtils.getAbsoluteFile(excelFile);
               
        try {
            if (xlFile != null) {
                String[] macros = null;
                if (premacro != null && !premacro.isEmpty() 
                    || runmacro != null && !runmacro.isEmpty()
                    || postmacro != null && !postmacro.isEmpty()) { //This if is just for speed issues.
                    macros = excelService.getMacros(xlFile);
                }
                if (premacro != null && !premacro.isEmpty()) {
                    if (!Arrays.asList(macros).contains(premacro)) {
                        final WorkflowNodeValidationMessage validationMessage =
                            new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.WARNING,
                                ExcelComponentConstants.PRE_MACRO,
                                Messages.errorWrongPreMacroRelative,
                                Messages.bind(Messages.errorWrongPreMacroAbsolute,
                                    ExcelComponentConstants.PRE_MACRO));
                        messages.add(validationMessage);
                    }
                }
                if (runmacro != null && !runmacro.isEmpty()) {
                    if (!Arrays.asList(macros).contains(runmacro)) {
                        final WorkflowNodeValidationMessage validationMessage =
                            new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.WARNING,
                                ExcelComponentConstants.RUN_MACRO,
                                Messages.errorWrongRunMacroRelative,
                                Messages.bind(Messages.errorWrongRunMacroAbsolute,
                                    ExcelComponentConstants.RUN_MACRO));
                        messages.add(validationMessage);
                    }
                }
                if (postmacro != null && !postmacro.isEmpty()) {
                    if (!Arrays.asList(macros).contains(postmacro)) {
                        final WorkflowNodeValidationMessage validationMessage =
                            new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.WARNING,
                                ExcelComponentConstants.POST_MACRO,
                                Messages.errorWrongPostMacroRelative,
                                Messages.bind(Messages.errorWrongPostMacroAbsolute,
                                    ExcelComponentConstants.POST_MACRO));
                        messages.add(validationMessage);
                    }
                }
            }
        } catch (ExcelException e) {
            // Just catching because ExcelException is not relevant at configuration validation time.
            LogFactory.getLog(getClass()).debug("Excel Exception (not relevant at configuration validation time.)");
        }

        return messages;
    }

}
