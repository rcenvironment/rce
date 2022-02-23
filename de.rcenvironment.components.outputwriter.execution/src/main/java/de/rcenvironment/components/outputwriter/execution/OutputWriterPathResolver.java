/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.execution;

import java.io.File;
import java.util.function.Consumer;

import org.eclipse.core.resources.ResourcesPlugin;

import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * 
 * A class to replace workspace placeholder in the root dir with the absolute workspace path.
 * 
 * @author Kathrin Schaffert
 * 
 */
public class OutputWriterPathResolver {

    private static final String FRONTSLASH = "/";

    private static final String BACKSLASHES = "\\";

    private final Consumer<String> logWarning;

    public OutputWriterPathResolver(Consumer<String> logWarning) {
        this.logWarning = logWarning;
    }

    public String adaptRootToAbsoluteRootIfProjectRelative(String rootToBeAdapted) throws ComponentException {

        String absoluteRoot = rootToBeAdapted;

        // if front and backslashes are mixed -> exception
        if (rootToBeAdapted.contains(FRONTSLASH) && rootToBeAdapted.contains(BACKSLASHES)) {
            throw new ComponentException(StringUtils.format(
                "Given path to file or directory could not be resolved, as it contains front and backslash as well: %s", rootToBeAdapted));
        }

        File file = new File(rootToBeAdapted);
        if (!file.isAbsolute()) {

            if (rootToBeAdapted.startsWith(OutputWriterComponentConstants.PH_WORKSPACE + FRONTSLASH)) {

                if (ResourcesPlugin.getWorkspace().getRoot().exists()) {

                    checkRelativePathForValidProject(rootToBeAdapted);

                    String workspacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
                    absoluteRoot = rootToBeAdapted.replace(OutputWriterComponentConstants.PH_WORKSPACE, workspacePath);
                } else {
                    throw new ComponentException(StringUtils.format("Failed to resolve root location '%s' "
                        + "because the workspace could not be determined. "
                        + "Note that in headless mode relative paths are not supported.", rootToBeAdapted));
                }
            } else {
                // // TODO 9.0.0: Remove this warning and make this fail,
                // // as relative paths without the explicit workspace placeholder are no longer supported
                this.logWarning.accept(StringUtils.format("Note that from version 9.0 on relative paths have to start explicitly "
                    + "with the prefix '%s'. Relative paths without this prefix are not resolved and result in a workflow"
                    + "failure.", OutputWriterComponentConstants.PH_WORKSPACE));
            }
        }

        return absoluteRoot;
    }

    private void checkRelativePathForValidProject(String relativePath) throws ComponentException {
        if (relativePath.split(FRONTSLASH).length < 2) {
            throw new ComponentException(StringUtils.format("Cannot resolve root location '%s' "
                + "because it contains no project.", relativePath));
        } else {
            String projectName = relativePath.split(FRONTSLASH)[1];
            if (!ResourcesPlugin.getWorkspace().getRoot().getProject(projectName).exists()) {
                throw new ComponentException(StringUtils.format("Failed to resolve root location '%s' "
                    + "because the given project '%s' could not be found.", relativePath, projectName));
            }
        }
    }

}
