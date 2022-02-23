/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration.api;

import java.util.List;
import java.util.Map;

import de.rcenvironment.core.gui.wizards.toolintegration.ToolIntegrationWizard;

/**
 * Interface for providing new pages to the {@link ToolIntegrationWizard}.
 * 
 * @author Sascha Zur
 */
public interface IntegrationWizardPageContributor {

    /**
     * @return the type of {@link ToolIntegrationContext} this contributor belongs to.
     */
    String getType();

    /**
     * @param configurationMap current configuration map
     * @return a list with all pages that shall be added to the wizard.
     */
    List<ToolIntegrationWizardPage> getAdditionalPagesList(Map<String, Object> configurationMap);
}
