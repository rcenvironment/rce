/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.introduction;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Action that opens links in external Browser. It is required because of target="_blank" in html does not work properly on Linux
 * distributions.
 * 
 * @author Riccardo Dusi
 */
public class NavigateToLink implements IIntroAction {

    private final Map<String, String> keyValueToURL = new HashMap<String, String>() {

        private static final long serialVersionUID = 1L;

        {
            put("news", "https://rcenvironment.de/#news");
            put("newsletter", "https://rcenvironment.de/pages/newsletter.html");
            put("twitter", "https://twitter.com/RCEnvironment");
            put("github", "https://github.com/rcenvironment");
        }
    };

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public void run(IIntroSite site, Properties properties) {
        final NavigateToLinkParameters parameters = NavigateToLinkParameters.createFromProperties(properties);
        try {
            openLink(parameters);
        } catch (PartInitException e) {
            logExceptionAsWarning(e);
        } catch (MalformedURLException e) {
            logExceptionAsWarningForMalformedURL(e);
        }
    }

    private void openLink(NavigateToLinkParameters parameters) throws PartInitException, MalformedURLException {
        if (keyValueToURL.containsKey(parameters.getKeyOfParameterValue())) {
            String mappedURL = keyValueToURL.get(parameters.getKeyOfParameterValue());
            PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(mappedURL));
        }
    }

    private void logExceptionAsWarning(CoreException e) {
        log.warn(StringUtils.format("Status: %s\nCause: %s", e.getStatus(), e.getCause()));
    }

    private void logExceptionAsWarningForMalformedURL(MalformedURLException e) {
        log.warn(StringUtils.format("Status: %s\nCause: %s", e.getStackTrace(), e.getCause()));
    }
}
