/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration.api;

import java.util.List;

/**
 * Registry for all {@link IntegrationWizardPageContributor}.
 * 
 * @author Sascha Zur
 */
public interface IntegrationWizardPageContributorRegistry {

    /**
     * @param contributor to add
     */
    void addPageContributor(IntegrationWizardPageContributor contributor);

    /**
     * @param contributor to remove
     */
    void removePageContributor(IntegrationWizardPageContributor contributor);

    /**
     * @return all currently registered {@link IntegrationWizardPageContributor}s
     */
    List<IntegrationWizardPageContributor> getAllContributors();
}
