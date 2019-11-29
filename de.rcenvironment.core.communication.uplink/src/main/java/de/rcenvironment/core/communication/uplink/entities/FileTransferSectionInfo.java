/*
 * Copyright 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.entities;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.rcenvironment.core.communication.uplink.common.internal.MessageType;

/**
 * Transports additional data as part of a {@link MessageType#FILE_TRANSFER_SECTION_START} message.
 *
 * @author Robert Mischke
 */
public class FileTransferSectionInfo implements Serializable {

    private static final long serialVersionUID = -6058161739304662346L;

    private List<String> directories;

    // deserialization constructor
    public FileTransferSectionInfo() {}

    public FileTransferSectionInfo(List<String> directories) {
        this.directories = directories;
    }

    public List<String> getDirectories() {
        return directories;
    }

    @JsonIgnore
    public Optional<List<String>> getDirectoriesAsOptional() {
        return Optional.ofNullable(directories);
    }

}
