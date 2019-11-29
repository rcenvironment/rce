/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import org.eclipse.jface.viewers.IFilter;

import de.rcenvironment.core.configuration.CommandLineArguments;
import de.rcenvironment.core.gui.workflow.AdvancedTabVisibilityHelper;
import de.rcenvironment.core.gui.workflow.parts.WorkflowRunNodePart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;


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
        if (object instanceof WorkflowRunNodePart){
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
