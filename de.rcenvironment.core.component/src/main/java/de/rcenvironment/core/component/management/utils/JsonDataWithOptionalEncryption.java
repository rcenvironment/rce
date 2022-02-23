/*
 * Copyright 2019-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.management.utils;

import java.io.Serializable;
import java.util.Map;

/**
 * A simple transfer object for JSON string data with optional encryption metadata. It is intended to be serialized, e.g. as part of another
 * JSON object.
 * <p>
 * There are two supported representations of the input string: public, and group-based encryption.
 * <p>
 * In public mode, the {@link #authData} field must be null, and the {@link #data} fields is the original input string.
 * <p>
 * If encryption is used, the {@link #data} fields is a string encoding (typically base64) of the input string's byte stream, encrypted with
 * a transient random symmetric key (typically 256-bit AES). This key is then encrypted with one or more provided symmetric keys, and stored
 * in the {@link #authData} map.
 *
 * @author Robert Mischke
 */
public class JsonDataWithOptionalEncryption implements Serializable {

    private static final long serialVersionUID = 9145522015802184335L;

    private Map<String, String> authData; // null = public access

    private String data; // serialized, and encrypted if non-public

    // required for deserialization
    public JsonDataWithOptionalEncryption() {}

    public JsonDataWithOptionalEncryption(String data, Map<String, String> authKeys) {
        this.authData = authKeys;
        this.data = data;
    }

    public Map<String, String> getAuthData() {
        return authData;
    }

    public void setAuthData(Map<String, String> authKeys) {
        this.authData = authKeys;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result;
        if (data != null) {
            result += data.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        JsonDataWithOptionalEncryption other = (JsonDataWithOptionalEncryption) obj;
        if (data == null) {
            if (other.data != null) {
                return false;
            }
        } else if (!data.equals(other.data)) {
            return false;
        }
        return true;
    }

}
