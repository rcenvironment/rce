/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPropertyListener;

import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage.Type;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessageStore;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;

/**
 * {@link WorkflowNodePropertySection} with capabilities to validate controls.
 * 
 * <p>
 * For information about how the validation works, see {@link WorkflowNodeValidatorSupport}.
 * </p>
 * 
 * @author Jascha Riedel
 * @author Christian Weiss
 * @author Kathrin Schaffert
 */
public abstract class ValidatingWorkflowNodePropertySection extends WorkflowNodePropertySection {

    protected static final String CONTROL_COLORIZED_KEY = "property.control.colorized";

    protected static final String CONTROL_COLOR_KEY = "property.control.color";

//    private final WorkflowNodeValidityStateListener validatorStateListener = createErrorStateUpdateListener();

//    private final WorkflowNodeValidationSupport validationSupport = new WorkflowNodeValidationSupport();
    
    private WorkflowNodeIdentifier componentId;
    

    private final ComponentValidationMessageStore messageStore = ComponentValidationMessageStore.getInstance();
    
    private IPropertyListener propertyListener = new IPropertyListener() {
        
        @Override
        public void propertyChanged(Object obj, int property) {
            if (property == WorkflowEditor.PROP_WORKFLOW_VAILDATION_FINISHED) {
                updateErrorStates();                    
            }
        }
    };

    public ValidatingWorkflowNodePropertySection() {
    }


    @Override
    protected final void afterInitializingModelBinding() {
        componentId = ((WorkflowNode) getConfiguration()).getIdentifierAsObject();
        updateErrorStates();
        afterInitializingModelBindingWithValidation();
        getPart().addPropertyListener(propertyListener);
    }

    protected void afterInitializingModelBindingWithValidation() {
        /* empty default implementation */
    }

    @Override
    protected void beforeTearingDownModelBinding() {
        try {
            beforeTearingDownModelBindingWithValidation();
        } finally {
            getPart().removePropertyListener(propertyListener);
        }
    }

    protected void beforeTearingDownModelBindingWithValidation() {
        /* empty default implementation */
    }

    protected void updateErrorStates() {
        final List<ComponentValidationMessage> messages = messageStore.getMessagesByComponentId(componentId.toString());
        updateErrorStates(messages);
    }

    protected ComponentValidationMessageStore getMessageStore() {
        return messageStore;
    }

    /**
     * The controls are traversed to highlight those which are linked to error messages represented
     * by {@link ComponentValidationMessage}s.
     * 
     * @param messages the {@link ComponentValidationMessage}s determined through validation
     */
    protected void updateErrorStates(final List<ComponentValidationMessage> messages) {
        updateErrorStates(messages, getComposite());
    }

    /**
     * The controls are traversed to highlight those which are linked to error messages represented
     * by {@link ComponentValidationMessage}s.
     * 
     * @param messages the {@link ComponentValidationMessage}s determined through validation
     * @param parent the actual parent composite
     */
    protected void updateErrorStates(final List<ComponentValidationMessage> messages, final Composite parent) {
        if (parent != null && !parent.isDisposed()) {
            for (final Control control : parent.getChildren()) {
                if (control.isDisposed()) {
                    continue;
                }
                if (control instanceof Composite) {
                    updateErrorStates(messages, (Composite) control);
                }
                final String key = (String) control.getData(CONTROL_PROPERTY_KEY);
                if (key != null) {
                    boolean valid = true;
                    for (final ComponentValidationMessage message : messages) {
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
                    if (valid && Boolean.TRUE.equals(colorized)) {
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


}
