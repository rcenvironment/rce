/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.common;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.component.api.ComponentGroupPathRules;
import de.rcenvironment.core.component.api.ComponentIdRules;
import de.rcenvironment.core.component.integration.IntegrationConstants;
import de.rcenvironment.core.component.integration.IntegrationContext;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Validator for the component description settings of integrated tools and workflows.
 * 
 * @author Kathrin Schaffert
 */
public final class ComponentDescriptionValidator {

    /** Warning Message for invalid icon path. */
    public static final String ICON_INVALID = "Icon path or file format is invalid. The default icon will be used.";

    private static final String TOOLNAME_INVALID = "The chosen component name is not valid. \n %s";

    private static final String TOOL_NAME_EXISTS =
        "An integrated component with the name '%s' is already configured within the current RCE profile.\n"
            + " Note that component names are not case sensitive.";

    private static final String ICON_INVALID_SPACES =
        "Icon path is invalid. Spaces are allowed, but cannot be the first or last character.";

    private static final String GROUPNAME_INVALID = "The chosen group name is not valid.\n %s";

    private static final String DOC_EXTENSION_NOT_VALID = "Documentation extension not valid. Valid extensions: ";

    private static final String VALID_EXTENSION_SEPERATOR = ", ";

    private static final String DOC_DOES_NOT_EXIST = "Documentation path is invalid.";

    private static final String LIMITATION_INVALID = "Limitation value for parallel executions is invalid.";

    private static final String LIMITATION_MAXIMUM_REACHED = "The maximum limitation value for parallel executions has been reached.";

    public ComponentDescriptionValidator() {
        super();
    }

    public Optional<String> validateName(Text toolNameText, Optional<String> nameOrigin, Collection<? extends String> usedToolnames) {
        Optional<String> validationResult = ComponentIdRules.validateComponentIdRules(toolNameText.getText());
        if (validationResult.isPresent()) {
            return Optional.of(StringUtils.format(TOOLNAME_INVALID, validationResult.get()));
        }
        String name = toolNameText.getText().trim();
        if (nameOrigin.isPresent() && name.equalsIgnoreCase(nameOrigin.get())) {
            return Optional.empty();
        }
        Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        set.addAll(usedToolnames);
        if (set.contains(name)) {
            set.removeIf((String s) -> !s.trim().equalsIgnoreCase(name));
            return Optional.of(StringUtils.format(TOOL_NAME_EXISTS, set.iterator().next()));
        }
        return Optional.empty();
    }

    public Optional<String> validateIcon(Text iconText, IntegrationContext context, String componentName) {
        if (iconText.getText() != null && !iconText.getText().isEmpty()) {
            if (iconText.getText().endsWith(" ") || iconText.getText().startsWith(" ")) {
                return Optional.of(ICON_INVALID_SPACES);
            }
            try {
                File icon = new File(iconText.getText());
                if (!icon.exists() && !icon.isAbsolute() && componentName != null && !componentName.isEmpty()) {
                    icon = new File(new File(
                        new File(context.getRootPathToToolIntegrationDirectory(), context.getNameOfToolIntegrationDirectory()),
                        componentName), iconText.getText());
                }
                Image image = ImageIO.read(icon);
                if (image == null) {
                    return Optional.of(ICON_INVALID);
                }
            } catch (IOException ex) {
                return Optional.of(ICON_INVALID);
            }
        }
        return Optional.empty();
    }

    public Optional<String> validateGroupPath(Text groupPathText) {
        Optional<String> validationResult = ComponentGroupPathRules.validateComponentGroupPathRules(groupPathText.getText());
        if (!groupPathText.getText().isEmpty() && validationResult.isPresent()) {
            return Optional.of(StringUtils.format(GROUPNAME_INVALID, validationResult.get()));
        }
        return Optional.empty();
    }

    public Optional<String> validateDoc(Text documentationText, IntegrationContext context, String componentName) {
        if (documentationText.getText() != null && !documentationText.getText().isEmpty()) {
            File doc = new File(documentationText.getText());
            if (!doc.exists() && !doc.isAbsolute() && componentName != null && !componentName.isEmpty()) {
                doc = new File(new File(
                    new File(new File(context.getRootPathToToolIntegrationDirectory(), context.getNameOfToolIntegrationDirectory()),
                        componentName),
                    IntegrationConstants.DOCS_DIR_NAME), documentationText.getText());
            }
            if (doc.exists()) {
                String extension = FilenameUtils.getExtension(doc.getAbsolutePath());
                if (!ArrayUtils.contains(IntegrationConstants.VALID_DOCUMENTATION_EXTENSIONS, extension.toLowerCase())) {
                    StringBuilder allowedExt = new StringBuilder(DOC_EXTENSION_NOT_VALID);
                    for (String current : IntegrationConstants.VALID_DOCUMENTATION_EXTENSIONS) {
                        allowedExt.append(current + VALID_EXTENSION_SEPERATOR);
                    }
                    return Optional.of(allowedExt.toString().substring(0, allowedExt.length() - VALID_EXTENSION_SEPERATOR.length()));
                }
            } else {
                return Optional.of(DOC_DOES_NOT_EXIST);
            }
        }
        return Optional.empty();
    }

    public Optional<String> validateParallelExecution(Text limitExecutionText, Button limitExecutionsButton) {
        if (limitExecutionsButton.getSelection()) {
            if (!limitExecutionText.getText().matches("\\d+")) {
                return Optional.of(LIMITATION_INVALID);
            }
            try {
                Integer.parseInt(limitExecutionText.getText());
                return Optional.empty();
            } catch (NumberFormatException e) {
                return Optional.of(LIMITATION_MAXIMUM_REACHED);
            }
        }
        return Optional.empty();
    }

}
