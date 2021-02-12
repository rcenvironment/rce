/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.validation.api;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Messages to inform about validation results of a {@link WorkflowNodeValidator}.
 * 
 * @author Christian Weiss
 * @author Jascha Riedel
 * @author Kathrin Schaffert
 */
public class ComponentValidationMessage {

    /**
     * The type of {@link ComponentValidationMessage}.
     * 
     * @author Christian Weiss
     */
    public enum Type {
        /** Warning message type. */
        WARNING,
        /** ERROR message type. */
        ERROR;
    }

    private final Type type;

    private final String property;

    private final String relativeMessage;

    private final String absoluteMessage;

    private final boolean revalidateOnWorkflowStart;

    public ComponentValidationMessage(Type type, final String property, final String relativeMessage, final String absoluteMessage) {
        this(type, property, relativeMessage, absoluteMessage, false);
    }

    public ComponentValidationMessage(Type type, final String property, final String relativeMessage, final String absoluteMessage,
        boolean revalidateOnWorkflowStart) {
        this.type = type;
        this.property = property;
        this.relativeMessage = relativeMessage;
        this.absoluteMessage = absoluteMessage;
        this.revalidateOnWorkflowStart = revalidateOnWorkflowStart;
    }

    public Type getType() {
        return type;
    }

    public String getProperty() {
        return property;
    }

    public String getRelativeMessage() {
        return relativeMessage;
    }

    public String getAbsoluteMessage() {
        return absoluteMessage;
    }

    public boolean isRevalidateOnWorkflowStart() {
        return revalidateOnWorkflowStart;
    }

    @Override
    public String toString() {
        if (property != null && !property.isEmpty()
            && relativeMessage != null && !relativeMessage.isEmpty()) {
            return StringUtils.format("%s: %s", property, relativeMessage);
        } else {
            return absoluteMessage;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        final int hashBooleanTrue = 1231;
        final int hashBooleanFalse = 1237;
        int result = 1;
        if (absoluteMessage != null) {
            result = prime * result + absoluteMessage.hashCode();
        } else {
            result = prime * result;
        }
        if (property != null) {
            result = prime * result + property.hashCode();
        } else {
            result = prime * result;
        }
        if (relativeMessage != null) {
            result = prime * result + relativeMessage.hashCode();
        } else {
            result = prime * result;
        }
        if (type != null) {
            result = prime * result + type.hashCode();
        } else {
            result = prime * result;
        }
        if (revalidateOnWorkflowStart) {
            result = prime * result + hashBooleanTrue;
        } else {
            result = prime * result + hashBooleanFalse;
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ComponentValidationMessage)) {
            return false;
        }
        ComponentValidationMessage other = (ComponentValidationMessage) obj;
        if (absoluteMessage == null) {
            if (other.absoluteMessage != null) {
                return false;
            }
        } else if (!absoluteMessage.equals(other.absoluteMessage)) {
            return false;
        }
        if (property == null) {
            if (other.property != null) {
                return false;
            }
        } else if (!property.equals(other.property)) {
            return false;
        }
        if (relativeMessage == null) {
            if (other.relativeMessage != null) {
                return false;
            }
        } else if (!relativeMessage.equals(other.relativeMessage)) {
            return false;
        }
        if (revalidateOnWorkflowStart != other.revalidateOnWorkflowStart) {
            return false;
        }
        return (type == other.type);
    }

}
