/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.workflowintegration.editor.mappingtreenodes;

import java.util.StringJoiner;

import org.eclipse.jface.viewers.TreeNode;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputDatumHandling;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * 
 * {@link TreeNode} extension representing mapping nodes.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 *
 */
public abstract class MappingNode extends TreeNode implements Comparable<MappingNode> {

    private static final String FILTER_DELIMITER = ";";

    private static final String DEFAULT_MAPPED_NAME_TEMPLATE = "%s_%s";

    private MappingType mappingType;

    private boolean checked = false;

    private boolean checkable = true;

    private boolean nameValid = true;

    protected MappingNode(ComponentNode parent) {
        super(parent);
        this.setParent(parent);
    }

    public void setCheckable(boolean checkable) {
        this.checkable = checkable;
        if (!checkable) {
            setChecked(true);
        }
    }

    public boolean isCheckable() {
        return checkable;
    }

    public static InputMappingNode createInputMappingNode(ComponentNode parent, String inputName, DataType dataType, String mappedName,
        InputDatumHandling handling, InputExecutionContraint constraint) {
        return new InputMappingNode(parent, inputName, dataType, mappedName, handling, constraint);
    }

    public static MappingNode createInputMappingNode(ComponentNode component, String inputName, DataType dataType,
        InputDatumHandling handling,
        InputExecutionContraint constraint) {
        return createInputMappingNode(component, inputName, dataType,
            getDefaultExternalName(inputName, component.getComponentName()), handling, constraint);
    }

    private static String getDefaultExternalName(String inputName, String componentName) {
        return StringUtils.format(DEFAULT_MAPPED_NAME_TEMPLATE, inputName, componentName);
    }

    public static OutputMappingNode createOutputMappingNode(ComponentNode parent, String inputName, DataType dataType, String mappedName) {
        return new OutputMappingNode(parent, inputName, dataType, mappedName);
    }

    public static OutputMappingNode createOutputMappingNode(ComponentNode parent, String inputName, DataType dataType) {
        return createOutputMappingNode(parent, inputName, dataType,
            getDefaultExternalName(inputName, parent.getComponentName()));
    }

    public abstract String getDetails();

    @Override
    public boolean hasChildren() {
        return false;
    }
    
    public MappingType getMappingType() {
        return mappingType;
    }

    public abstract String getInternalName();

    public void setMappingType(MappingType mappingType) {
        this.mappingType = mappingType;
    }

    public abstract DataType getDataType();

    public abstract String getExternalName();

    public abstract void setExternalName(String mappedName);

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((getInternalName() == null) ? 0 : getInternalName().hashCode());
        result = prime * result + ((getDataType() == null) ? 0 : getDataType().hashCode());
        result = prime * result + ((getExternalName() == null) ? 0 : getExternalName().hashCode());
        result = prime * result + ((getMappingType() == null) ? 0 : getMappingType().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MappingNode other = (MappingNode) obj;
        if (!getParent().equals(other.getParent())) {
            return false;
        }
        if (getInternalName() == null) {
            if (other.getInternalName() != null) {
                return false;
            }
        } else if (!getInternalName().equals(other.getInternalName())) {
            return false;
        }
        if (getDataType() != other.getDataType()) {
            return false;
        }
        if (getMappingType() != other.getMappingType()) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(MappingNode o) {
        return getInternalName().compareToIgnoreCase(o.getInternalName());
    }

    public void setNameValid(boolean isValid) {
        this.nameValid = isValid;
    }

    public boolean isNameValid() {
        return nameValid;
    }

    public String getFilterString() {
        StringJoiner builder = new StringJoiner(FILTER_DELIMITER);
        builder.add(getInternalName());
        builder.add(getMappingType().name());
        builder.add(getDataType().getDisplayName());
        builder.add(getDetails());
        builder.add(getExternalName());
        return builder.toString();
    }

    public void setDefaultExternalName() {
        if (getParent() instanceof ComponentNode) {
            this.setExternalName(getDefaultExternalName());
        }
    };

    public String getDefaultExternalName() {
        if (getParent() instanceof ComponentNode) {
            return getDefaultExternalName(getInternalName(), ((ComponentNode) getParent()).getComponentName());
        }
        return "";
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append(((ComponentNode) getParent()).getComponentName());
        builder.append(":");
        builder.append(getInternalName());
        builder.append("->");
        builder.append(getExternalName());
        builder.append(FILTER_DELIMITER);
        builder.append(getDataType());
        builder.append(FILTER_DELIMITER);
        builder.append(getMappingType());
        builder.append("]");
        return builder.toString();
    }

}

