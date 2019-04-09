/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.core.gui.wizards.toolintegration.api.IntegrationWizardPageContributor;
import de.rcenvironment.core.gui.wizards.toolintegration.api.IntegrationWizardPageContributorRegistry;

/**
 * Implementation of {@link IntegrationWizardPageContributorRegistry}.
 * 
 * @author Sascha Zur
 */
public class IntegrationWizardPageContributorRegistryImpl implements IntegrationWizardPageContributorRegistry {

    private final List<IntegrationWizardPageContributor> contributors =
        Collections.synchronizedList(new LinkedList<IntegrationWizardPageContributor>());

    @Override
    public void addPageContributor(IntegrationWizardPageContributor contributor) {
        contributors.add(contributor);
    }

    @Override
    public void removePageContributor(IntegrationWizardPageContributor contributor) {
        contributors.remove(contributor);
    }

    @Override
    public List<IntegrationWizardPageContributor> getAllContributors() {
        return contributors;
    }

}
