/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration.cpacs;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.component.integration.cpacs.CpacsToolIntegrationConstants;
import de.rcenvironment.core.gui.wizards.toolintegration.api.IntegrationWizardPageContributor;
import de.rcenvironment.core.gui.wizards.toolintegration.api.ToolIntegrationWizardPage;

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
        return CpacsToolIntegrationConstants.CPACS_TOOL_INTEGRATION_CONTEXT_TYPE;
    }

    @Override
    public List<ToolIntegrationWizardPage> getAdditionalPagesList(Map<String, Object> configurationMap) {
        List<ToolIntegrationWizardPage> pages = new LinkedList<ToolIntegrationWizardPage>();
        pages.add(new CpacsConfigurationPage(Messages.cpacsPageTitle, configurationMap));
        return pages;
    }
}
