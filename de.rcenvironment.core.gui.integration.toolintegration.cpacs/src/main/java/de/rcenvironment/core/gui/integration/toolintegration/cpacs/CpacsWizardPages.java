/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.toolintegration.cpacs;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.component.integration.IntegrationContextType;
import de.rcenvironment.core.gui.integration.toolintegration.api.IntegrationWizardPageContributor;
import de.rcenvironment.core.gui.integration.toolintegration.api.ToolIntegrationWizardPage;

/**
 * Contribution of additional CPACS tool specific configurations to the tool integration wizard.
 * 
 * @author Jan Flink
 */
public class CpacsWizardPages implements IntegrationWizardPageContributor {

    public CpacsWizardPages() {

    }

    @Override
    public String getType() {
        return IntegrationContextType.CPACS.toString();
    }

    @Override
    public List<ToolIntegrationWizardPage> getAdditionalPagesList(Map<String, Object> configurationMap) {
        List<ToolIntegrationWizardPage> pages = new LinkedList<ToolIntegrationWizardPage>();
        pages.add(new CpacsConfigurationPage(Messages.cpacsPageTitle, configurationMap));
        return pages;
    }
}
