/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationSupport;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidityStateEvent;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidityStateListener;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage.Type;

/**
 * {@link WorkflowNodePropertySection} with capabilities to validate controls.
 * 
 * <p>
 * For information about how the validation works, see {@link WorkflowNodeValidatorSupport}.
 * </p>
 * 
 * @author Christian Weiss
 */
public abstract class ValidatingWorkflowNodePropertySection extends WorkflowNodePropertySection {

    private static final String CONTROL_COLORIZED_KEY = "property.control.colorized";

    private static final String CONTROL_COLOR_KEY = "property.control.color";

    private final WorkflowNodeValidityStateListener validatorStateListener = createErrorStateUpdateListener();

    private final WorkflowNodeValidationSupport validationSupport = new WorkflowNodeValidationSupport();

    public ValidatingWorkflowNodePropertySection() {
        validationSupport.addWorkflowNodeValidityStateListener(getErrorStateUpdateListener());
    }

    /**
     * Returns the {@link WorkflowNodeValidityStateListener} updating the error state of Controls.
     * 
     * @return a {@link WorkflowNodeValidityStateListener}
     */
    protected final WorkflowNodeValidityStateListener getErrorStateUpdateListener() {
        return validatorStateListener;
    }

    /**
     * Creates the {@link WorkflowNodeValidityStateListener} updating the error state of Controls.
     * 
     * @return a {@link WorkflowNodeValidityStateListener}
     */
    protected WorkflowNodeValidityStateListener createErrorStateUpdateListener() {
        return new ErrorStateUpdateListener();
    }

    @Override
    protected final void afterInitializingModelBinding() {
        validationSupport.setWorkflowNode((WorkflowNode) getConfiguration());
        updateErrorStates();
        afterInitializingModelBindingWithValidation();
    }

    protected void afterInitializingModelBindingWithValidation() {
        /* empty default implementation */
    }

    @Override
    protected final void beforeTearingDownModelBinding() {
        try {
            beforeTearingDownModelBindingWithValidation();
        } finally {
            validationSupport.setWorkflowNode(null);
        }
    }

    protected void beforeTearingDownModelBindingWithValidation() {
        /* empty default implementation */
    }

    protected void updateErrorStates() {
        final List<WorkflowNodeValidationMessage> messages = new LinkedList<WorkflowNodeValidationMessage>(validationSupport.getMessages());
        updateErrorStates(messages);
    }

    /**
     * The controls are traversed to highlight those which are linked to error messages represented
     * by {@link WorkflowNodeValidationMessage}s.
     * 
     * @param messages the {@link WorkflowNodeValidationMessage}s determined through validation
     */
    protected void updateErrorStates(final List<WorkflowNodeValidationMessage> messages) {
        updateErrorStates(messages, getComposite());
    }

    /**
     * The controls are traversed to highlight those which are linked to error messages represented
     * by {@link WorkflowNodeValidationMessage}s.
     * 
     * @param messages the {@link WorkflowNodeValidationMessage}s determined through validation
     * @param parent the actual parent composite
     */
    protected void updateErrorStates(final List<WorkflowNodeValidationMessage> messages, final Composite parent) {
        if (parent != null) {
            for (final Control control : parent.getChildren()) {
                if (control instanceof Composite) {
                    updateErrorStates(messages, (Composite) control);
                }
                final String key = (String) control.getData(CONTROL_PROPERTY_KEY);
                if (key != null) {
                    boolean valid = true;
                    for (final WorkflowNodeValidationMessage message : messages) {
                        if (key.equals(message.getProperty())) {
                            control.setData(CONTROL_COLORIZED_KEY, true);
                            if (control.getData(CONTROL_COLOR_KEY) == null) {
                                control.setData(CONTROL_COLOR_KEY, control.getBackground());
                            }
                            if (message.getType() == Type.WARNING) {
                                control.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));
                            } else {
                                control.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
                            }
                            control.setToolTipText(message.getRelativeMessage());
                            valid = false;
                            break;
                        }
                    }
                    final Boolean colorized = (Boolean) control.getData(CONTROL_COLORIZED_KEY);
                    if (valid && colorized != null && colorized) {
                        control.setData(CONTROL_COLORIZED_KEY, false);
                        control.setBackground((Color) control.getData(CONTROL_COLOR_KEY));
                        control.setData(CONTROL_COLOR_KEY, null);
                        control.setToolTipText("");
                    }
                }
            }
        }
    }

    @Override
    public void refreshSection() {
        super.refreshSection();
        refreshBeforeValidation();
        updateErrorStates();
    }

    protected void refreshBeforeValidation() {
        /* empty default implementation */
    }

    /**
     * {@link WorkflowNodeValidityStateListener} triggering the update of the the error states of
     * the controls.
     * 
     * @author Christian Weiss
     */
    private class ErrorStateUpdateListener implements WorkflowNodeValidityStateListener {

        @Override
        public void handleWorkflowNodeValidityStateEvent(final WorkflowNodeValidityStateEvent event) {
            updateErrorStates();
        }

    };

}
