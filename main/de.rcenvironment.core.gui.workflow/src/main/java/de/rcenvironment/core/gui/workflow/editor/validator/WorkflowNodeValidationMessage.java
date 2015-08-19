/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.validator;

import java.io.Serializable;

import de.rcenvironment.core.utils.common.StringUtils;


/**
 * Messages to inform about validation results of a {@link WorkflowNodeValidator}.
 * 
 * @author Christian Weiss
 */
public class WorkflowNodeValidationMessage implements Serializable {

    /**
     * The type of {@link WorkflowNodeValidationMessage}.
     * 
     * @author Christian Weiss
     */
    public enum Type {
        /** Warning message type. */
        WARNING,
        /** ERROR message type. */
        ERROR;
    }

    private static final long serialVersionUID = 3558625170986798920L;

    private final Type type;

    private final String property;

    private final String relativeMessage;

    private final String absoluteMessage;

    private final boolean revalidateOnWorkflowStart;

    @Deprecated
    public WorkflowNodeValidationMessage(final String property, final String relativeMessage, final String absoluteMessage) {
        this(Type.ERROR, property, relativeMessage, absoluteMessage);
    }

    public WorkflowNodeValidationMessage(Type type, final String property, final String relativeMessage, final String absoluteMessage) {
        this(type, property, relativeMessage, absoluteMessage, false);
    }

    public WorkflowNodeValidationMessage(Type type, final String property, final String relativeMessage, final String absoluteMessage,
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
    public boolean equals(final Object obj) {
        boolean result = super.equals(obj);
        if (obj instanceof WorkflowNodeValidationMessage) {
            final WorkflowNodeValidationMessage other = (WorkflowNodeValidationMessage) obj;
            final String thisString = "" + property + relativeMessage + absoluteMessage;
            final String otherString = "" + other.property + other.relativeMessage + other.absoluteMessage;
            result = thisString.equals(otherString);
        }
        return result;
    }

    @Override
    public int hashCode() {
        // FIXME 5.0: this doesn't make much sense; concatenating first, then applying nullSafe()? - misc_ro 
        return StringUtils.nullSafe(property + relativeMessage + absoluteMessage).hashCode();
    }

    @Override
    public String toString() {
        if (property != null && !property.isEmpty()
                && relativeMessage != null && !relativeMessage.isEmpty()) {
            return String.format("%s: %s", property, relativeMessage);
        } else {
            return absoluteMessage;
        }
    }

}
