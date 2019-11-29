/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.equinox.security.storage.provider.IProviderHints;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.configuration.SecureStorageSection;
import de.rcenvironment.core.configuration.SecureStorageService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.utils.common.IdGenerator;

/**
 * An implementation of the {@link SecureStorageService} API backed by Eclipse's {@link SecurePreferencesFactory}.
 * <p>
 * Note that while the JavaDoc of {@link ISecurePreferences} does not explicitly state whether implementations are guaranteed to be thread
 * safe, the interface is explicitly modeled after <code>org.osgi.service.prefs.Preferences</code>, which is declared to be thread safe.
 * Therefore, thread safety is assumed.
 * <p>
 * TODO 9.0.0 open design decision: should failures due to an unavailable store be restricted to each write/read operation, opening up the
 * option of unlocking the store later, or should failures deactivate the storage completely until the next restart?
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 * @author Brigitte Boden
 * @author Robert Mischke
 */
@Component
public final class SecureStorageServiceImpl implements SecureStorageService {

    // the actual secure storage file; located in <profile>/internal/
    // (protected access for unit tests)
    protected static final String SECURE_SETTINGS_FILE_NAME = "settings.secure.dat";

    protected static final String SECURE_SETTINGS_BACKUP_FILE_NAME = "settings.secure.bak";

    // the file containing the generated per-user (ie cross-profile) secure storage password
    // (protected access for unit tests)
    protected static final String KEY_FILE_NAME = "secure_storage_key";

    protected static final String PASSWORD_OVERRIDE_PROPERTY = "rce.overrideSecureStoragePassword";

    private static final int EXPECTED_KEY_LENGTH = 64;

    private ConfigurationService configurationService;

    private File secureStorageFile;

    private File secureStorageBackupFile;

    private ISecurePreferences rootNode;

    private final Log log = LogFactory.getLog(getClass());

    @Activate
    protected void activate() throws IOException {
        final String storePassword = determineStorePassword();

        // note: the Eclipse JavaDoc states that the URL must point to a directory, but it actually has to be a file - misc_ro
        File profileInternalDir = configurationService.getConfigurablePath(ConfigurablePathId.PROFILE_INTERNAL_DATA);

        secureStorageFile = new File(profileInternalDir, SECURE_SETTINGS_FILE_NAME);

        secureStorageBackupFile = new File(profileInternalDir, SECURE_SETTINGS_BACKUP_FILE_NAME);

        if (secureStorageFile.isFile()) {
            if (secureStorageBackupFile.isFile()) {
                Files.delete(secureStorageBackupFile.toPath());
            }
            Files.copy(secureStorageFile.toPath(), secureStorageBackupFile.toPath());
        }

        rootNode = openSecurePreferencesStoreWithPassword(storePassword);
        try {
            // perform a test write to create the file early and verify that is actually writable
            rootNode.put("writeTest", "RCE Secure Storage", false); // false = not encrypted; still encoded instead of clear text
            rootNode.flush();
        } catch (StorageException e) {
            throw new IOException("Test write to Secure Storage failed - aborting startup");
        }

        checkExistingEntriesRecursively(rootNode);

        log.debug("Secure Storage initialized");
    }

    private void checkExistingEntriesRecursively(ISecurePreferences node) {
        String nodeName = node.name();
        if (nodeName == null) {
            nodeName = "<root>";
        }
        for (String key : node.keys()) {
            try {
                node.get(key, null);
                log.debug(StringUtils.format("Successfully tested decryption of existing entry %s/%s", nodeName, key));
            } catch (StorageException e) {
                final String formatString;
                if (e.getMessage() != null && e.getMessage().contains("bad key")) {
                    formatString = "Failed to decrypt existing Secure Storage entry %s/%s as it was written with a "
                        + "different Secure Storage login/password. "
                        + "You can open the Secure Storage file %s (while RCE is not running) and delete the line "
                        + "starting with this entry to remove this warning.";
                } else {
                    formatString = "Failed to decrypt existing Secure Storage entry %s/%s due to an unknown error. "
                        + "You can open the Secure Storage file %s (while RCE is not running) and delete the line "
                        + "beginning with this entry to remove this warning.";
                }
                log.warn(StringUtils.format(formatString, nodeName, key, secureStorageFile.getAbsolutePath()));
            }
        }
        for (String subNode : node.childrenNames()) {
            ISecurePreferences section = node.node(subNode);
            checkExistingEntriesRecursively(section);
        }
    }

    @Reference
    protected void bindConfigurationService(ConfigurationService newConfigurationService) {
        this.configurationService = newConfigurationService;
    }

    @Override
    public SecureStorageSection getSecureStorageSection(String sectionId) throws IOException {
        return new SecureStorageSectionSecurePreferencesImpl(sectionId, rootNode.node(sectionId));
    }

    private String determineStorePassword() throws IOException {

        // use trimmed, non-empty override property or environment variable if set
        String passwordOverrideProperty = System.getProperty(PASSWORD_OVERRIDE_PROPERTY);
        if (passwordOverrideProperty == null) {
            // also check environment variables in this case
            passwordOverrideProperty = System.getenv(PASSWORD_OVERRIDE_PROPERTY);
        }
        if (passwordOverrideProperty != null) {
            passwordOverrideProperty = passwordOverrideProperty.trim();
            if (!passwordOverrideProperty.isEmpty()) {
                return passwordOverrideProperty;
            }
        }

        // standard case: auto-generate a password if it does not exist, and store it in a cross-profile key file
        return readOrGenerateAutomaticPassword();
    }

    private String readOrGenerateAutomaticPassword() throws IOException {
        File keyFile = new File(configurationService.getConfigurablePath(ConfigurablePathId.SHARED_USER_SETTINGS_ROOT),
            KEY_FILE_NAME);

        if (keyFile.exists()) {
            String fileContent = FileUtils.readFileToString(keyFile).trim();
            if (fileContent.length() != EXPECTED_KEY_LENGTH) {
                throw new IOException("Unexpected content in key file " + keyFile.getAbsolutePath()
                    + ": expected an auto-generated password of " + EXPECTED_KEY_LENGTH + " characters");
            }
            log.debug("Reusing existing cross-profile keyfile");
            return fileContent; // success
        } else {
            String password = IdGenerator.secureRandomHexString(EXPECTED_KEY_LENGTH);
            FileUtils.writeStringToFile(keyFile, password);
            log.debug("Generated a new cross-profile keyfile");
            return password;
        }
    }

    /**
     * Opens and returns RCE's secure storage.
     * 
     * @param password if no gui is required and the password is given in another way
     * @return secure storage as {@link ISecurePreferences} object
     * @throws IOException on error
     */
    private ISecurePreferences openSecurePreferencesStoreWithPassword(String password) throws IOException {
        // With options.put(IProviderHints.DEFAULT_PASSWORD, newKey), a password can be provided,
        // with options.put(IProviderHints.PROMPT_USER, false) the password GUI will not be shown.
        Map<String, Object> options = new HashMap<>();
        options.put(IProviderHints.DEFAULT_PASSWORD, new PBEKeySpec(password.toCharArray()));
        options.put(IProviderHints.PROMPT_USER, false);
        return openSecurePreferencesStoreInternal(options);
    }

    /**
     * @param options the options, as expected by the {@link ISecurePreferences} API; can be null
     * 
     * @return secure storage as {@link ISecurePreferences} object
     * @throws IOException on failure to open the secure storage
     */
    private ISecurePreferences openSecurePreferencesStoreInternal(Map<String, Object> options) throws IOException {
        final URL secureStorageFileUrl;
        try {
            // The toURL() method is deprecated, because it does not automatically escape characters that are illegal in URLs. However, the
            // Equinox documentation states that "Similarly to the rest of the Equinox, URLs passed as an argument must not be encoded,
            // meaning that spaces should stay as spaces, not as "%x20"." Therefore, we are using this deprecated method here. - rode_to
            secureStorageFileUrl = secureStorageFile.toURL();
        } catch (MalformedURLException e) {
            throw new IOException("Failed to open/unlock the Secure Storage", e);
        }

        final ISecurePreferences securePreferences =
            org.eclipse.equinox.security.storage.SecurePreferencesFactory.open(secureStorageFileUrl, options);
        if (securePreferences == null) {
            throw new IOException("Failed to open/unlock the Secure Storage: received a null reference");
        }

        return securePreferences;
    }
}
