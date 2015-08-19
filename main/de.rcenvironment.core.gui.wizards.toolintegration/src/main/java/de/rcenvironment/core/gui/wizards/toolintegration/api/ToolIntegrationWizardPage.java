/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration.api;

import java.util.Map;

import org.eclipse.jface.wizard.WizardPage;

import de.rcenvironment.core.gui.wizards.toolintegration.ToolIntegrationWizard;

/**
 * Extended interface for a {@link WizardPage} for the {@link ToolIntegrationWizard}.
 * 
 * @author Sascha Zur
 */
public abstract class ToolIntegrationWizardPage extends WizardPage {

    protected ToolIntegrationWizardPage(String pageName) {
        super(pageName);
    }

    /**
     * Sets a new configuration map that is chosen by the user and is used to update the page.
     * 
     * @param newConfigurationMap from the wizard
     */
    public abstract void setConfigMap(Map<String, Object> newConfigurationMap);

    /**
     * Updates the page before it is shown. This is for example for refreshing some gui elements.
     */
    public abstract void updatePage();
}
