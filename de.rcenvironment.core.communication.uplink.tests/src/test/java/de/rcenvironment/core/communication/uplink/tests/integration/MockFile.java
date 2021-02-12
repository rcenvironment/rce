/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.tests.integration;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A test helper class representing a virtual input or output file.
 *
 * @author Robert Mischke
 */
class MockFile {

    public final String relativePath;

    // a separate field to allow testing of mismatches between announced and actual data length in the future
    public final long announcedSize;

    public final byte[] content;

    MockFile(String relativePath, long announcedSize, byte[] content) {
        this.relativePath = relativePath;
        this.announcedSize = announcedSize;
        this.content = content;
    }

    public String getSignature() {
        return StringUtils.format("%s:%d:%s", relativePath, announcedSize, new String(content));
    }
}
