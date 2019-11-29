/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.introduction;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Action that persists the selection status of the checkbox shown in the welcome screen in the preference store.
 * 
 * @author Riccardo Dusi
 * @author Alexander Weinert (refactoring and cleanup)
 */
public class SaveUserInput implements IIntroAction {

    private static final String RELATIVE_PATH_TO_SCRIPT = "content/script/intro.js";

    private static final Log log = LogFactory.getLog(SaveUserInput.class);

    private static String getPathToScriptFile() {
        Bundle bundle = Platform.getBundle("de.rcenvironment.core.gui.introduction");
        URL scriptFileURL = bundle.getEntry(RELATIVE_PATH_TO_SCRIPT);
        // resolves the searched file in the temp. unpacked JAR
        try {
            // resolves the searched file in the temp. unpacked JAR
            URL resolvedFileURL = FileLocator.toFileURL(scriptFileURL);
            // We need to use the 3-arg constructor of URI in order to properly escape file system chars
            URI resolvedURI = new URI(resolvedFileURL.getProtocol(), resolvedFileURL.getPath(), null);
            String resolvedPath = new File(resolvedURI).toPath().toAbsolutePath().toString();
//            log.info("Path to script file intro.js: " + resolvedPath);
            return resolvedPath;
        } catch (URISyntaxException | IOException e) {
            log.error("Unable to locate script file 'intro.js'", e);
            return "";
        }

    }

    /**
     * Executes every time the checkbox "Do not show welcome screen again." is clicked.
     */
    @Override
    public void run(IIntroSite site, Properties properties) {
        final SaveUserInputParameters parameters = SaveUserInputParameters.createFromProperties(properties);
        // REVIEW (AW): Why is the value first stored to preferences and subsequently read from preferences again?
        storeCheckboxValueToPreferences(parameters.isCheckboxChecked());
        persistUserPreferenceToScriptFile();
    }

    // REVIEW (AW): It would be possible and cleaner to pass the user choice into this method instead of having it determine this itself
    // from preferences
    private void persistUserPreferenceToScriptFile() {
        try {
            final boolean preferencesContainShowIntroKey = preferencesContainShowIntroKey();
            final WelcomeScreenScriptFile scriptFile = WelcomeScreenScriptFile.createFromAbsolutePath(getPathToScriptFile());
            scriptFile.persistUserChoice(preferencesContainShowIntroKey);
        } catch (BackingStoreException e) {
            logExceptionAsError(e);
        }
    }

    private boolean preferencesContainShowIntroKey() throws BackingStoreException {
        final IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode("org.eclipse.ui");
        preferences.flush();
        final List<String> prefKeyList = Arrays.asList(preferences.keys());
        // REVIEW (AW): This used to be the string literal "showIntro" instead of the string constant
        // REVIEW (AW): Only the existence of the key is tested, but not the value itself
        return prefKeyList.contains(IWorkbenchPreferenceConstants.SHOW_INTRO);
    }

    private void storeCheckboxValueToPreferences(final boolean userHasCheckedCheckbox) {
        PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.SHOW_INTRO, !userHasCheckedCheckbox);
    }
    
    private void logExceptionAsError(BackingStoreException e) {
        log.error("Preferences operation could not be completed", e);
    }
}
