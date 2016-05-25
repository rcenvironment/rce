/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.gui.view;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.rcenvironment.rce.components.excel.commons.ChannelValue;


/**
 * ContentProvider for channel values.
 *
 * @author Markus Kunde
 */
public class ChannelValueContentProvider implements IStructuredContentProvider {

    @Override
    public void dispose() {}

    @Override
    public void inputChanged(Viewer arg0, Object arg1, Object arg2) {}

    @Override
    public Object[] getElements(Object inputElement) {
        @SuppressWarnings("unchecked")
        List<ChannelValue> channelvalues = (List<ChannelValue>) inputElement;
        return channelvalues.toArray();
    }
}
