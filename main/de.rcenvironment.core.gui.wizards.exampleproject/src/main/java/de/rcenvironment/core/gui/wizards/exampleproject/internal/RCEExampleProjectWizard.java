/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.wizards.exampleproject.internal;

import de.rcenvironment.core.gui.wizards.exampleproject.NewExampleProjectWizard;

/**
 * Wizard for the RCE examples project.
 * @author Sascha Zur
 */
public class RCEExampleProjectWizard extends NewExampleProjectWizard{

    @Override
    public String getPluginID() {
        return "de.rcenvironment.core.gui.wizards.exampleproject";
    }

    @Override
    public String getTemplateFoldername() {
        return "workflows_examples";
    }

    @Override
    public String getProjectDefaultName() {
        return "Workflow Examples Project";
    }

}
