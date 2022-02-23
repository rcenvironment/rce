/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.expressions.PropertyTester;
import org.osgi.service.log.LoggerFactory;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.gui.workflow.view.WorkflowRunEditor;
import de.rcenvironment.core.gui.workflow.view.WorkflowRunEditorService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * PropertyTester for the workflow control buttons (resume, pause, cancel) in the toolbar. The tester evaluates the button activation
 * depending on the current workflow state.
 *
 * @author Jan Flink
 * @author Alexander Weinert
 */
public class RuntimeEditorPropertyTester extends PropertyTester {

    private final WorkflowRunEditorService workflowRunEditorService;

    private final Log log;

    public RuntimeEditorPropertyTester() {
        workflowRunEditorService = ServiceRegistry.createAccessFor(this).getService(WorkflowRunEditorService.class);
        log = LogFactory.getLog(getClass());
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        final Optional<WorkflowRunEditor> wre = workflowRunEditorService.getCurrentWorkflowRunEditor();
        if (!wre.isPresent()) {
            return false;
        }

        final WorkflowState state = wre.get().getCurrentEditorWorkflowState();

        switch (property) {
        case "enableResume":
            return state.isResumable();
        case "enablePause":
            return state.isPausable();
        case "enableCancel":
            return state.isCancellable();
        default:
            log.error(
                StringUtils.format("Unknown property: '%s'. Expected one of ['enableResume', 'enablePause', 'enableCancel']", property));
            // In this case, we do not know what property we should test for, so we fail safely and disable the requested operation
            return false;
        }
    }
}
