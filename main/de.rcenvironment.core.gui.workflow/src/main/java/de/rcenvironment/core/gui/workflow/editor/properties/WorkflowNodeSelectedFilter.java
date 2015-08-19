/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import org.eclipse.jface.viewers.IFilter;

import de.rcenvironment.core.gui.workflow.AdvancedTabVisibilityHelper;
import de.rcenvironment.core.gui.workflow.parts.ReadOnlyWorkflowNodePart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;
import de.rcenvironment.core.start.common.CommandLineArguments;


/**
 * Filter class to display the general property tab for all workflow nodes.
 *
 * @author Heinrich Wendel
 * @author Oliver Seebach
 */
public class WorkflowNodeSelectedFilter implements IFilter {

    @Override
    public boolean select(Object object) {
        // Always show info tab in read only workflow editor
        if (object instanceof ReadOnlyWorkflowNodePart){
            return true;
        } else if (object instanceof WorkflowNodePart){
            // Show advanced tab when either globally enabled by flag or triggered at runtime
            if (CommandLineArguments.isShowAdvancedTab()){
                return true;
            }
            boolean advancedVisible = AdvancedTabVisibilityHelper.isShowAdvancedTab();
            if (advancedVisible){
                return true;
            }
        }
        // Don't show advanced tab per default
        return false;
    }


}
