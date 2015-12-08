/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.console;

import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.execution.api.ConsoleRow;

/**
 * Used to get colored text.
 * 
 * @author Doreen Seider
 */
public class DecoratedConsoleLabelProvider extends DecoratingLabelProvider implements ITableLabelProvider {

    private final Color red = Display.getDefault().getSystemColor(SWT.COLOR_RED);
    
    private final Color yellow = Display.getDefault().getSystemColor(SWT.COLOR_DARK_YELLOW);

    private final Color black = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
    
    private final Color gray = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
    
    private ITableLabelProvider provider;
    
    private ILabelDecorator decorator;

    public DecoratedConsoleLabelProvider(ILabelProvider provider, ILabelDecorator decorator) {
        super(provider, decorator);
        this.provider = (ITableLabelProvider) provider;
        this.decorator = decorator;
    }

    @Override
    public Image getColumnImage(Object element, int columnIndex) {
        Image image = provider.getColumnImage(element, columnIndex);
        if (decorator != null) {
            Image decorated = decorator.decorateImage(image, element);
            if (decorated != null) {
                return decorated;
            }
        }
        return image;
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
        String text = provider.getColumnText(element, columnIndex);
        if (decorator != null) {
            String decorated = decorator.decorateText(text, element);
            if (decorated != null) {
                return decorated;
            }
        }
        return text;
    }
    
    @Override
    public Color getForeground(Object element) {
        
        Color result = null;

        if (element instanceof ConsoleRow) {
            ConsoleRow consoleRow = (ConsoleRow) element;

            switch (consoleRow.getType()) {
            case WORKFLOW_ERROR:
            case COMPONENT_ERROR:
            case TOOL_ERROR:
                result = red;
                break;
            case COMPONENT_WARN:
                result = yellow;
                break;
            case LIFE_CYCLE_EVENT:
                result = gray;
                break;
            default:
                result = black;
                break;
            }
        }

        return result;
    }
}
