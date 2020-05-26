/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.entities;

import java.io.Serializable;

/**
 * Transports a file's metadata.
 *
 * @author Robert Mischke
 */
public class FileHeader implements Serializable {

    private static final long serialVersionUID = 4018390412906695717L;

    private long size;

    private String path;

    public FileHeader() {
        // for deserialization
    }

    public FileHeader(long size, String path) {
        this.size = size;
        this.path = path;
    }

    public long getSize() {
        return size;
    }

    public String getPath() {
        return path;
    }

}
