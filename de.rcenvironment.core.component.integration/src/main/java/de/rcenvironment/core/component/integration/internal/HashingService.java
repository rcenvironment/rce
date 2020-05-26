/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.integration.internal;

import org.apache.commons.codec.digest.DigestUtils;
import org.osgi.service.component.annotations.Component;

/**
 * Wrapper for hashing methods required by {@link IconHelper}. Moved to this wrapper class in order to make {@link IconHelper} testable.
 * 
 * @author Alexander Weinert
 */
@Component(service = HashingService.class)
public class HashingService {

    /**
     * Wraps DigestUtils.md5Hex.
     * 
     * @param content The values to be hashed.
     * @return A hexadecimal representation of the MD5 hash of the given content
     */
    public String md5Hex(byte[] content) {
        return DigestUtils.md5Hex(content);
    }

}
