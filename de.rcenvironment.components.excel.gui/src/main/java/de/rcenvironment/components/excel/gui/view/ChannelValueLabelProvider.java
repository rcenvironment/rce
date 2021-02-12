/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.gui.view;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.components.excel.common.ChannelValue;
import de.rcenvironment.components.excel.common.ExcelComponentConstants;
import de.rcenvironment.components.excel.common.ExcelUtils;


/**
 * ChannelValueLabelProvider.
 *
 * @author Markus Kunde
 */
public class ChannelValueLabelProvider extends LabelProvider implements ITableLabelProvider {
    
    @Override
    public Image getColumnImage(Object arg0, int arg1) {
        return null;
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
        ChannelValue channelValue = (ChannelValue) element;
        
        String returnValue = null;
        
        switch (columnIndex) {
        case 0:
            returnValue =
                ExcelUtils.smallTableToString(channelValue.getValues(), ExcelComponentConstants.STRINGLINESEPARATOR,
                    ExcelComponentConstants.TABLELINESEPARATOR);
            break;
        case 1:
            returnValue = channelValue.getChannelName();
            break;
        case 2:
            returnValue = String.valueOf(channelValue.getIteration());
            break;
        case 3:   
            if (channelValue.isInputValue()) {
                returnValue = Messages.inputChannelName;
            } else {
                returnValue = Messages.outputChannelName;
            }
            break;
        default:
            throw new RuntimeException("Error during column text initialization.");
        }
        
        return returnValue;
    }
    

}
