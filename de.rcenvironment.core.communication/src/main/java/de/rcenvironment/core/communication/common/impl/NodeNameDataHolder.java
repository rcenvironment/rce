/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.common.impl;

import java.util.Optional;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.CommonIdBase;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A mutable holder for the current naming data associated with certain node identifier.
 * <p>
 * IMPORTANT: This class is NOT synchronized; this must be ensured by the classes/services accessing it.
 *
 * @author Robert Mischke
 */
public final class NodeNameDataHolder {

    /**
     * The name to display for nodes with encrypted name data that have not been decrypted/unlocked yet.
     */
    public static final String UNDECRYPED_NAME_PLACEHOLDER = "<no authorization>";

    private String resolvedName;

    private String encryptionGroupId; // TODO decide: discard once resolved?

    private String encryptedName; // TODO decide: discard once resolved?

    private boolean decryptionUnavailable; // either the local node is not member of the group, or decryption failed

    public NodeNameDataHolder() {}

    /**
     * Convenience constructor for representing a resolved name.
     * 
     * @param resolvedName the resolved name to set
     */
    public NodeNameDataHolder(String resolvedName) {
        this();
        this.resolvedName = resolvedName;
    }

    public void setResolvedName(String resolvedName) {
        this.resolvedName = resolvedName;
    }

    /**
     * Stores encrypted name data for subsequent decryption.
     * 
     * @param encryptedNameData the encrypted name data; supposed to match a certain internal format, which is parsed by this method
     */
    public void setEncryptedNameData(String encryptedNameData) {
        final String[] parts = StringUtils.splitAndUnescape(encryptedNameData);
        if (parts.length == 2) {
            this.encryptionGroupId = parts[0];
            this.encryptedName = parts[1];
        } else {
            LogFactory.getLog(getClass()).error("Ignoring invalid encrypted node name data: " + encryptedNameData);
            this.encryptionGroupId = null;
            this.encryptedName = null;
        }
    }

    public boolean isResolved() {
        return resolvedName != null;
    }

    public boolean isDecryptionUnavailable() {
        return decryptionUnavailable;
    }

    public boolean isEncrypted() {
        return encryptedName != null;
    }

    public String getResolvedName() {
        return resolvedName;
    }

    public Optional<String> getOptionalResolvedName() {
        return Optional.ofNullable(resolvedName);
    }

    public Optional<String> getOptionalEncryptionGroupId() {
        return Optional.ofNullable(encryptionGroupId);
    }

    public Optional<String> getOptionalEncryptedName() {
        return Optional.ofNullable(encryptedName);
    }

    @Override
    public synchronized String toString() {
        if (isResolved()) {
            return resolvedName; // plain text or previously decrypted
        } else if (isEncrypted()) {
            return UNDECRYPED_NAME_PLACEHOLDER;
        } else {
            return CommonIdBase.DEFAULT_DISPLAY_NAME;
        }
    }
}
