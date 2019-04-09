/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.api;

/**
 * Interface for method calls affecting the configuration of the embedded SSH server.
 * 
 * @author Robert Mischke
 */
public interface EmbeddedSshServerControl {

    /**
     * Defines a key-value pair that is made available (announced) to SSH clients. Typically, this is used to publish protocol version or
     * capability information. The default way to access this information as a client is by inspecting the SSH server banner, where each
     * entry is appended as a "<key>/<value>" segment. No special escaping is performed; this must be taken care of by callers of this
     * method.
     * 
     * If there already is an entry with this key, it is replaced with the new value.
     * 
     * @param key the identifier to set the version for
     * @param version the version to announce
     */
    void setAnnouncedVersionOrProperty(String key, String version);

}
