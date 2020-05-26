/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.excel.execution.validator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.excel.common.ExcelAddress;
import de.rcenvironment.components.excel.common.ExcelComponentConstants;
import de.rcenvironment.components.excel.common.ExcelException;
import de.rcenvironment.components.excel.common.ExcelService;
import de.rcenvironment.components.excel.common.ExcelUtils;
import de.rcenvironment.components.excel.common.ExcelServiceAccess;
import de.rcenvironment.components.excel.execution.Messages;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.spi.AbstractComponentValidator;

/**
 * Validator for Excel Component that connects the standard Excel Validator with
 * the Marco Validator.
 * 
 * @author Jascha Riedel
 *
 */
public class ExcelComponentValidator extends AbstractComponentValidator {

    @Override
    public String getIdentifier() {
        return ExcelComponentConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateComponentSpecific(ComponentDescription componentDescription) {
        List<ComponentValidationMessage> messages = new ArrayList<ComponentValidationMessage>();

        messages.addAll(validateExcelPart(componentDescription));
        messages.addAll(validateMacroPart(componentDescription));

        return messages;
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
            ComponentDescription componentDescription) {
        return null;
    }

    private List<ComponentValidationMessage> validateExcelPart(ComponentDescription componentDescription) {
        ExcelService excelService = ExcelServiceAccess.get();

        final List<ComponentValidationMessage> messages = new ArrayList<ComponentValidationMessage>();

        // Get all relevant validation items
        String excelFile = getProperty(componentDescription, ExcelComponentConstants.XL_FILENAME);
        boolean driver = Boolean.valueOf(getProperty(componentDescription, ExcelComponentConstants.DRIVER));

        // Check
        File xlFile = ExcelUtils.getAbsoluteFile(excelFile);

        if (xlFile == null || !xlFile.exists() || !excelService.isValidExcelFile(xlFile)) {
            final ComponentValidationMessage validationMessage = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.ERROR, ExcelComponentConstants.XL_FILENAME,
                    Messages.errorNoExcelFileRelative, Messages.bind(Messages.errorNoExcelFileRelative, null), true);

            messages.add(validationMessage);
        }

        if (driver && getOutputs(componentDescription).isEmpty()) {
            final ComponentValidationMessage validationMessage = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.WARNING, ExcelComponentConstants.DRIVER,
                    Messages.errorNoOutputAsDriverRelative,
                    Messages.bind(Messages.errorNoOutputAsDriverAbsolute, ExcelComponentConstants.DRIVER));
            messages.add(validationMessage);
        }

        // TODO Check of endpoints runs to long for execution in GUI-thread.
        // Validation should be called in another thread on a higher level
        /*
         * for (final EndpointDescription inputName: getInputs()) {
         * messages.addAll(testChannelMetaData(xlFile, inputName)); }
         * 
         * for (final EndpointDescription outputName: getOutputs()) {
         * messages.addAll(testChannelMetaData(xlFile, outputName)); }
         */

        return messages;
    }

    private List<ComponentValidationMessage> testChannelMetaData(final File xlFile, EndpointDescription endpointDesc) {
        final List<ComponentValidationMessage> messages = new LinkedList<ComponentValidationMessage>();

        String address = endpointDesc.getMetaDataValue(ExcelComponentConstants.METADATA_ADDRESS);
        final ComponentValidationMessage validationMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, ExcelComponentConstants.METADATA_ADDRESS,
                Messages.errorNoMetaDataAddressRelative,
                Messages.bind(Messages.errorNoMetaDataAddressAbsolute, ExcelComponentConstants.METADATA_ADDRESS));
        try {
            new ExcelAddress(xlFile, address);
        } catch (ExcelException e) {
            messages.add(validationMessage);
        }

        return messages;
    }

    private List<ComponentValidationMessage> validateMacroPart(ComponentDescription componentDescription) {
        ExcelService excelService = ExcelServiceAccess.get();

        final List<ComponentValidationMessage> messages = new ArrayList<ComponentValidationMessage>();

        // Get all relevant validation items
        String excelFile = getProperty(componentDescription, ExcelComponentConstants.XL_FILENAME);
        String premacro = getProperty(componentDescription, ExcelComponentConstants.PRE_MACRO);
        String runmacro = getProperty(componentDescription, ExcelComponentConstants.RUN_MACRO);
        String postmacro = getProperty(componentDescription, ExcelComponentConstants.POST_MACRO);

        // Check
        File xlFile = ExcelUtils.getAbsoluteFile(excelFile);

        try {
            if (xlFile != null) {
                String[] macros = null;
                if (premacro != null && !premacro.isEmpty() || runmacro != null && !runmacro.isEmpty()
                        || postmacro != null && !postmacro.isEmpty()) { // This
                                                                        // if is
                                                                        // just
                                                                        // for
                                                                        // speed
                                                                        // issues.
                    macros = excelService.getMacros(xlFile);
                }
                if (premacro != null && !premacro.isEmpty()) {
                    if (!Arrays.asList(macros).contains(premacro)) {
                        final ComponentValidationMessage validationMessage = new ComponentValidationMessage(
                                ComponentValidationMessage.Type.WARNING, ExcelComponentConstants.PRE_MACRO,
                                Messages.errorWrongPreMacroRelative,
                                Messages.bind(Messages.errorWrongPreMacroAbsolute, ExcelComponentConstants.PRE_MACRO));
                        messages.add(validationMessage);
                    }
                }
                if (runmacro != null && !runmacro.isEmpty()) {
                    if (!Arrays.asList(macros).contains(runmacro)) {
                        final ComponentValidationMessage validationMessage = new ComponentValidationMessage(
                                ComponentValidationMessage.Type.WARNING, ExcelComponentConstants.RUN_MACRO,
                                Messages.errorWrongRunMacroRelative,
                                Messages.bind(Messages.errorWrongRunMacroAbsolute, ExcelComponentConstants.RUN_MACRO));
                        messages.add(validationMessage);
                    }
                }
                if (postmacro != null && !postmacro.isEmpty()) {
                    if (!Arrays.asList(macros).contains(postmacro)) {
                        final ComponentValidationMessage validationMessage = new ComponentValidationMessage(
                                ComponentValidationMessage.Type.WARNING, ExcelComponentConstants.POST_MACRO,
                                Messages.errorWrongPostMacroRelative, Messages.bind(
                                        Messages.errorWrongPostMacroAbsolute, ExcelComponentConstants.POST_MACRO));
                        messages.add(validationMessage);
                    }
                }
            }
        } catch (ExcelException e) {
            // Just catching because ExcelException is not relevant at
            // configuration validation time.
            LogFactory.getLog(getClass()).debug("Excel Exception (not relevant at configuration validation time.)");
        }

        return messages;
    }

}
