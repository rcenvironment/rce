/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.gui.view;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.rcenvironment.components.excel.common.ChannelValue;


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
