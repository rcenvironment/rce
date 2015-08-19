/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.CommandStack;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource2;

import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;

/**
 * Class that maps configuration of a component onto the IPropertySource interface.
 * 
 * @author Sascha Zur
 */

// TODO seid_do 20140213: Is this class implemented or just a stub?
public class LabelPropertySource implements IPropertySource2 {

    public LabelPropertySource(CommandStack stack, WorkflowLabel label) {}

    @Override
    public Object getEditableValue() {
        return this;
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {

        List<IPropertyDescriptor> descriptors = new ArrayList<IPropertyDescriptor>();

        return descriptors.toArray(new IPropertyDescriptor[] {});
    }

    @Override
    public boolean isPropertyResettable(final Object key) {
        return false;
    }

    @Override
    public Object getPropertyValue(Object key) {

        return "";
    }

    @Override
    public boolean isPropertySet(Object key) {
        return false;
    }

    @Override
    public void resetPropertyValue(Object key) {}

    @Override
    public void setPropertyValue(Object arg0, Object arg1) {

    }

}
