/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.console;

import java.text.SimpleDateFormat;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.component.execution.api.ConsoleRow;

/**
 * Provides the concrete label texts to display and images if required.
 * 
 * @author Enrico Tappert
 * @author Doreen Seider
 */
public class ConsoleLabelProvider extends LabelProvider implements ITableLabelProvider {

    private Image stdErrImage = ImageDescriptor.createFromURL(
        ConsoleView.class.getResource("/resources/icons/stderr16.gif")).createImage(); //$NON-NLS-1$

    private Image stdOutImage = ImageDescriptor.createFromURL(
        ConsoleView.class.getResource("/resources/icons/stdout16.gif")).createImage(); //$NON-NLS-1$

    private Image metaInfoImage = ImageDescriptor.createFromURL(
        ConsoleView.class.getResource("/resources/icons/metainfo16.gif")).createImage(); //$NON-NLS-1$

    private SimpleDateFormat timestampDateFormat;

    /**
     * Default constructor.
     */
    public ConsoleLabelProvider() {
        timestampDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
    }

    @Override
    public Image getColumnImage(Object element, int columnIndex) {

        Image result = null;

        if (element instanceof ConsoleRow) {
            ConsoleRow consoleRow = (ConsoleRow) element;

            if (0 == columnIndex) {
                // level column
                if (ConsoleRow.Type.STDERR == consoleRow.getType()) {
                    result = stdErrImage;
                } else if (ConsoleRow.Type.STDOUT == consoleRow.getType()) {
                    result = stdOutImage;
                } else if (ConsoleRow.Type.COMPONENT_OUTPUT == consoleRow.getType()
                    || ConsoleRow.Type.LIFE_CYCLE_EVENT == consoleRow.getType()) {
                    result = metaInfoImage;
                }
            }
        }

        return result;
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
        String returnValue = ""; //$NON-NLS-1$

        if (element instanceof ConsoleRow) {
            ConsoleRow row = (ConsoleRow) element;

            switch (columnIndex) {
            // date and time
            case 1:
                returnValue = timestampDateFormat.format(row.getTimestamp());
                break;
            // text
            case 2:
                if (row.getType() != ConsoleRow.Type.LIFE_CYCLE_EVENT) {
                    returnValue = row.getPayload();
                } else if (ConsoleRow.WorkflowLifecyleEventType.valueOf(row.getPayload())
                    == ConsoleRow.WorkflowLifecyleEventType.COMPONENT_TERMINATED) {
                    returnValue = String.format("------ End of component '%s' ------", row.getComponentName());
                } else {
                    returnValue = row.getPayload();                    
                }
                break;
            // component
            case 3:
                returnValue = row.getComponentName();
                break;
            // workflow
            case 4:
                returnValue = String.valueOf(row.getWorkflowName());
                break;
            default:
                // shouldn't happen
                break;
            }
        }

        return returnValue;
    }

    @Override
    public void dispose() {
        stdOutImage.dispose();
        stdOutImage = null;
        stdErrImage.dispose();
        stdErrImage = null;
        metaInfoImage.dispose();
        metaInfoImage = null;
    }
}
