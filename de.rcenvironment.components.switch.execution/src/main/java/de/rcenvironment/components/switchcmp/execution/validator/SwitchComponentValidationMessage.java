/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.execution.validator;

import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;

/**
 * Messages to inform about validation results of a {@link SwitchComponentValidator}.
 * 
 * @author Kathrin Schaffert
 */
public final class SwitchComponentValidationMessage extends ComponentValidationMessage {

    private Integer rowIndex;

    private String errorMessage;

    private SwitchComponentValidationMessage(Type type, String property, Integer rowIndex, String rawMessage, String errorMessage) {
        super(type, property, errorMessage, errorMessage);
        this.errorMessage = rawMessage;
        this.rowIndex = rowIndex;
    }

    /**
     * 
     * Creates a Switch {@link ComponentValidationMessage}.
     * 
     * @param type type of the error message
     * @param property Property Key
     * @param rawMessage error message string of a certain table row
     * @param rowIndex row number of the condition table row belonging to the errorMessage
     * 
     * @return SwitchComponentValidationMessage
     */
    public static SwitchComponentValidationMessage create(Type type, String property, String rawMessage, Integer rowIndex) {
        final String errorMessage = "Condition " + rowIndex + ": " + "\n" + rawMessage;
        return new SwitchComponentValidationMessage(type, property, rowIndex, rawMessage, errorMessage);
    }

    /**
     * Returns the Condition Table Row Number of the current {@link SwitchComponentValidationMessage}.
     * 
     * @return Condition Table Row Number
     */
    public int getConditionTableRowNumber() {
        return rowIndex;
    }

    public String getToolTipMessage() {
        return errorMessage;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof SwitchComponentValidationMessage)) {
            return false;
        }
        SwitchComponentValidationMessage other = (SwitchComponentValidationMessage) obj;
        if (errorMessage == null) {
            if (other.errorMessage != null) {
                return false;
            }
        } else if (!errorMessage.equals(other.errorMessage)) {
            return false;
        }
        if (rowIndex == null) {
            if (other.rowIndex != null) {
                return false;
            }
        } else if (!rowIndex.equals(other.rowIndex)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        if (errorMessage != null) {
            result = prime * result + errorMessage.hashCode();
        } else {
            result = prime * result;
        }
        if (rowIndex != null) {
            result = prime * result + rowIndex.hashCode();
        } else {
            result = prime * result;
        }
        return result;
    }

    public void setConditionTableRowNumber(int rowIndexParam) {
        this.rowIndex = rowIndexParam;

    }

}
