/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.security.storage.ISecurePreferences;

import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;

/**
 * RCE specific wrapper class for Eclipse's {@link SecurePreferencesFactory}.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 * @author Robert Mischke
 */
public final class SecurePreferencesFactory {

    private static final String SECURE_SETTINGS_FILE_NAME = "settings.secure.dat";

    private static final Log LOGGER = LogFactory.getLog(SecurePreferencesFactory.class);

    private static ConfigurationService configService;

    private static File secureStorageFile;

    /**
     * Because of OGSi.
     */
    @Deprecated
    public SecurePreferencesFactory() {}

    /**
     * Opens and returns RCE's secure storage.
     * 
     * @return secure storage as {@link ISecurePreferences} object
     * @throws IOException on error
     */
    public static ISecurePreferences getSecurePreferencesStore() throws IOException {
        try {
            // The toURL() method is deprecated, because it does not automatically escape characters that are illegal in URLs. However, the
            // Equinox documentation states that "Similarly to the rest of the Equinox, URLs passed as an argument must not be encoded,
            // meaning that spaces should stay as spaces, not as "%x20"." Therefore, we are using this deprecated method here. - rode_to
            return org.eclipse.equinox.security.storage.SecurePreferencesFactory.open(secureStorageFile.toURL(), null);
        } catch (MalformedURLException e) {
            LOGGER.error("Opening RCE's secure storage failed", e);
            throw new IOException(e);
        }
    }

    /**
     * Opens and returns RCE's secure storage.
     * 
     * @param password if no gui is required and the password is given in another way
     * @return secure storage as {@link ISecurePreferences} object
     * @throws IOException on error
     */
    public static ISecurePreferences getSecurePreferencesStore(String password) throws IOException {
        // For this method, the optiones from the SecurePreferencesFactory.open method should be
        // used.
        // with options.put(IProviderHints.DEFAULT_PASSWORD, newKey), a password can be provided and
        // with
        // options.put(IProviderHints.PROMPT_USER, false) the password gui will not be shown.
        throw new UnsupportedOperationException();

        // try {
        // Map<String, Object> options = new HashMap<String, Object>();
        // PBEKeySpec newKey = new
        // PBEKeySpec(SecretKeyFactory.getInstance("").generateSecret("test"));
        // options.put(IProviderHints.DEFAULT_PASSWORD, newKey);
        // UnsupportedOperationException
        // options.put(IProviderHints.PROMPT_USER, false);
        // return org.eclipse.equinox.security.storage.SecurePreferencesFactory.open(new File(new
        // SimpleConfigurationService()
        // .getPlatformHome() + "/secure_storage").toURI().toURL(), options);
        // }
        // } catch (MalformedURLException e) {
        // LOGGER.error("Opening RCE's secure storage failed", e);
        // throw new IOException(e);
    }

    protected void bindConfigurationService(ConfigurationService newConfigurationService) {
        configService = newConfigurationService;
        // note: the Eclipse JavaDoc states that the URL must point to a directory, but it actually has to be a file - misc_ro
        secureStorageFile = new File(configService.getConfigurablePath(ConfigurablePathId.PROFILE_INTERNAL_DATA),
            SECURE_SETTINGS_FILE_NAME);
    }

}
