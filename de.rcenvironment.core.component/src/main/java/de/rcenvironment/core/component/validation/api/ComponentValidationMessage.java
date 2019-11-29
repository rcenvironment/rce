/*
 * Copyright 2006-2019 DLR, Germany
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
    public boolean equals(final Object obj) {
        boolean result = super.equals(obj);
        if (obj instanceof ComponentValidationMessage) {
            final ComponentValidationMessage other = (ComponentValidationMessage) obj;
            final String thisString = "" + property + relativeMessage + absoluteMessage;
            final String otherString = "" + other.property + other.relativeMessage + other.absoluteMessage;
            result = thisString.equals(otherString);
        }
        return result;
    }

    @Override
    public int hashCode() {
        // TODO (p3) review: this doesn't make much sense; concatenating first, then applying nullSafe()? - misc_ro 
        return StringUtils.nullSafe(property + relativeMessage + absoluteMessage).hashCode();
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

}
