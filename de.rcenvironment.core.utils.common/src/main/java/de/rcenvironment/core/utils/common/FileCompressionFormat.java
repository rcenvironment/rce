/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;

/**
 * 
 * Supported file compression formats for the {@link FileCompressionService}.
 *
 * @author Thorsten Sommer
 * @author Robert Mischke
 */
public enum FileCompressionFormat {

    /**
     * The zip format.
     */
    ZIP {

        @Override
        public String getArchiveStreamType() {
            return ArchiveStreamFactory.ZIP;
        }

        @Override
        public boolean applyGzipToArchiveStream() {
            return false;
        }

        @Override
        public String getDefaultFileExtension() {
            return "zip";
        }
    },

    /**
     * The tar format.
     */
    TAR_GZ {

        @Override
        public String getArchiveStreamType() {
            return ArchiveStreamFactory.TAR;
        }

        @Override
        public boolean applyGzipToArchiveStream() {
            return true;
        }

        @Override
        public String getDefaultFileExtension() {
            return "tgz";
        }
    };

    /**
     * 
     * This method returns the necessary compression format.
     * 
     * @return the compression format
     */
    public abstract String getArchiveStreamType();

    /**
     * This method signals the case where the actual archive stream (e.g. TAR) is wrapped into an outer gzip stream. The main use case is
     * .tar.gz/.tgz .
     * 
     * @return true if the final data stream should be wrapped / unwrapped using gzip.
     */
    public abstract boolean applyGzipToArchiveStream();

    /**
     * 
     * This method returns the default archive file extension. Currently only used for unit tests.
     * 
     * @return the extension WITHOUT a leading dot.
     */
    public abstract String getDefaultFileExtension();
}
