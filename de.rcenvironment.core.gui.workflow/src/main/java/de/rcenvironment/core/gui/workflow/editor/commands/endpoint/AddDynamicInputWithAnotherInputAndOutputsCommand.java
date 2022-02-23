/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.editor.commands.endpoint;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * Adds one single input, another input with given suffix and two outputs - one with the same name
 * as the input and one with the same name as the input + given suffix.
 * 
 * @author Sascha Zur
 */
public class AddDynamicInputWithAnotherInputAndOutputsCommand extends AddDynamicInputWithOutputsCommand {

    private final String addDynInputId;
    
    private final String inputNameSuffix;

    private final String groupForOtherInput;
    
    private Map<String, String> metaDataInputWithSuffix;

    public AddDynamicInputWithAnotherInputAndOutputsCommand(String dynEndpointId, String addDynInputId,
        String inputNameSuffix, String addDynOutputId, String outputNameSuffix, String name, DataType type,
        Map<String, String> metaData, String groupForOtherInput, Refreshable... panes) {
        super(dynEndpointId, addDynOutputId, outputNameSuffix, name, type, metaData, panes);
        this.addDynInputId = addDynInputId;
        this.inputNameSuffix = inputNameSuffix;
        this.groupForOtherInput = groupForOtherInput;
        this.metaDataInputWithSuffix = new HashMap<>();
        metaDataInputWithSuffix.putAll(metaData);
    }

    @Override
    public void execute() {
        InputWithOutputsCommandUtils.addInputWithSuffix(getProperties(), addDynInputId, name, type, inputNameSuffix,
            groupForOtherInput, metaDataInputWithSuffix);
        super.execute();
    }

    @Override
    public void undo() {
        InputWithOutputsCommandUtils.removeInputWithSuffix(getProperties(), name, inputNameSuffix);
        super.undo();
    }
    /**
     * Adds the given meta data to the current ones. 
     * @param additionalMetaDataInputWithSuffix to add.
     */
    public void addMetaDataToInputWithSuffix(Map<String, String> additionalMetaDataInputWithSuffix) {
        this.metaDataInputWithSuffix.putAll(additionalMetaDataInputWithSuffix);
    }
    
}
