/*
 * Copyright (C) 2006-2015 DLR, Germany
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
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Provides the concrete label texts to display and images if required.
 * 
 * @author Enrico Tappert
 * @author Doreen Seider
 * @author Sascha Zur
 */
public class ConsoleLabelProvider extends LabelProvider implements ITableLabelProvider {

    private static final int MAXIMUM_LENGTH_OF_DISPLAYED_CONSOLEROW = 180;

    private Image errImage = ImageManager.getInstance().getSharedImage(StandardImages.ERROR_16);

    private Image warningImage = ImageManager.getInstance().getSharedImage(StandardImages.WARNING_16);

    private Image infoImage = ImageManager.getInstance().getSharedImage(StandardImages.INFORMATION_16);

    private Image outImage = ImageDescriptor.createFromURL(
        ConsoleView.class.getResource("/resources/icons/stdout16.gif")).createImage();

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
                if (ConsoleRow.Type.WORKFLOW_ERROR == consoleRow.getType()
                    || ConsoleRow.Type.COMPONENT_ERROR == consoleRow.getType()
                    || ConsoleRow.Type.TOOL_ERROR == consoleRow.getType()) {
                    result = errImage;
                } else if (ConsoleRow.Type.COMPONENT_WARN == consoleRow.getType()) {
                    result = warningImage;
                } else if (ConsoleRow.Type.TOOL_OUT == consoleRow.getType()
                    || ConsoleRow.Type.COMPONENT_INFO == consoleRow.getType()
                    || ConsoleRow.Type.LIFE_CYCLE_EVENT == consoleRow.getType()) {
                    result = outImage;
                } else if (ConsoleRow.Type.LIFE_CYCLE_EVENT == consoleRow.getType()) {
                    result = infoImage;
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
            case 0:
                if (row.getType() == Type.TOOL_OUT || row.getType() == Type.TOOL_ERROR) {
                    returnValue = "Tool";
                } else if (row.getType() == Type.WORKFLOW_ERROR) {
                    returnValue = "Workflow";
                } else {
                    returnValue = "Component";
                }
                break;
            // date and time
            case 1:
                returnValue = timestampDateFormat.format(row.getTimestamp());
                break;
            // text
            case 2:
                if (row.getType() == ConsoleRow.Type.LIFE_CYCLE_EVENT
                    && ConsoleRow.WorkflowLifecyleEventType
                        .valueOf(row.getPayload()) == ConsoleRow.WorkflowLifecyleEventType.COMPONENT_TERMINATED) {
                    returnValue = StringUtils.format("------ End of component '%s' ------", row.getComponentName());
                } else {
                    returnValue = row.getPayload();
                }
                if (returnValue.length() > MAXIMUM_LENGTH_OF_DISPLAYED_CONSOLEROW) {
                    returnValue = returnValue.substring(0, MAXIMUM_LENGTH_OF_DISPLAYED_CONSOLEROW);
                    returnValue += " ... (line is cut, it is too long; copy and paste it to get it at full length)";
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
        outImage.dispose();
    }
}
