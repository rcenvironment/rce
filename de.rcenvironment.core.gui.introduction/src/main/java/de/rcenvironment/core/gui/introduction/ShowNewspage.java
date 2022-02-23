/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.introduction;

import java.net.MalformedURLException;
import java.net.URL;
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
 * Action that opens the news page of RCE in a external browser.
 * 
 * @author Riccardo Dusi
 */
public class ShowNewspage implements IIntroAction {

    private static final String URL_STRING = "https://rcenvironment.de/#news";

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public void run(IIntroSite arg0, Properties arg1) {
        try {
            PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(URL_STRING));
        } catch (PartInitException e) {
            logExceptionAsWarning(e);
        } catch (MalformedURLException e) {
            logExceptionAsWarningForMalformedURL(e);
        }
    }

    private void logExceptionAsWarning(CoreException e) {
        log.warn(StringUtils.format("Status: %s\nCause: %s", e.getStatus(), e.getCause()));
    }

    private void logExceptionAsWarningForMalformedURL(MalformedURLException e) {
        log.warn(StringUtils.format("Status: %s\nCause: %s", e.getStackTrace(), e.getCause()));
    }

}
