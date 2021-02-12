/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import com.fasterxml.jackson.annotation.JsonIgnore;
import de.rcenvironment.core.embedded.ssh.api.SshAccount;

/**
 * Default {@link SshAccount} implementation.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 * @author Brigitte Boden (added public key authentication)
 */
public class SshAccountImpl implements SshAccount {

    private String loginName;

    private String password;

    private String passwordHash;

    private String publicKey;

    @JsonIgnore
    private PublicKey publicKeyObj;

    private String role = "";

    // allow to disable/suspend accounts without deleting them
    private boolean enabled = true;

    public SshAccountImpl() {} // for JSON serialization

    public SshAccountImpl(String username, String password, String passwordHash, String publicKey, String role) {
        this.loginName = username;
        this.password = password;
        this.passwordHash = passwordHash;
        this.publicKey = publicKey;
        this.role = role;
        parsePublicKey();
    }

    /**
     * Method to validate the SshUser.
     * 
     * @param roles - List of roles
     * @param log the log instance to send validation failures to (as warnings)
     * @return true if valid, else false
     */
    public boolean validate(List<SshAccountRole> roles, Log log) {
        boolean isValid = true;
        boolean noMatchingRole = true;

        // every user has a name (that is not the empty String)
        if (loginName == null || loginName.isEmpty()) {
            log.warn("Found a user without username");
            isValid = false;
        }

        // warn on deprecated clear-text passwords, but do not fail the validation
        if (isValid && password != null) {
            log.warn("SSH user \"" + loginName + "\" has an insecure clear-text password. "
                + "Refer to the RCE User Guide on how to change it to a secure format.");
        }

        // every user has a password or a public key
        if ((password == null || password.isEmpty())
            && (passwordHash == null || passwordHash.isEmpty())
            && (publicKey == null || publicKey.isEmpty())) {
            log.warn("User \"" + loginName + "\" does not have a password, password hash, or public key");
            isValid = false;
        }

        if (password != null && passwordHash != null) {
            log.warn("User \"" + loginName + "\" has both a clear-text and a hashed password at the same time");
            isValid = false;
        }

        // ensure public key is valid (can be parsed to public key object)
        if (publicKey != null && !publicKey.isEmpty() && publicKeyObj == null) {
            log.warn("SSH User \"" + loginName + "\" has an invalid public key (only RSA keys are valid)");
            isValid = false;
        }

        // ensure role is not null
        if (role == null) {
            log.warn("Changed role for user \"" + loginName + "\" from null to empty string");
            role = "";
        }

        // role of user exist
        for (SshAccountRole curRole : roles) {
            if (role.equals(curRole.getRoleName())) {
                noMatchingRole = false;
                break;
            }
        }
        if (noMatchingRole) {
            log.warn("Non-existing role \"" + role + "\" configured for user \"" + loginName
                + "\". Default permissions (\"help\", \"exit\", \"version\") will be used.");
        }
        return isValid;
    }

    @Override
    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getPublicKey() {
        return publicKey;
    }

    /**
     * Sets the string representation of the public key and parses it to a key object.
     * 
     * @param publicKey the string representation of the public key
     */
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
        parsePublicKey();
    }

    @JsonIgnore
    @Override
    public PublicKey getPublicKeyObj() {
        return publicKeyObj;
    }

    @Override
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rcenvironment.core.embedded.ssh.api.SshAccount#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private void parsePublicKey() {
        if (publicKey != null && !publicKey.isEmpty()) {
            try {
                // Parse known key string to a PublicKey object.
                byte[] encKey = Base64.decodeBase64(publicKey.split(" ")[1]);
                DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(encKey));

                byte[] header = readElement(inputStream);
                String pubKeyFormat = new String(header);
                if (pubKeyFormat.equals("ssh-rsa")) {
                    byte[] publicExponent = readElement(inputStream);
                    byte[] modulus = readElement(inputStream);

                    KeySpec spec = new RSAPublicKeySpec(new BigInteger(modulus), new BigInteger(publicExponent));
                    KeyFactory kf = KeyFactory.getInstance("RSA");
                    publicKeyObj = kf.generatePublic(spec);
                }
            } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException | ArrayIndexOutOfBoundsException e) {
                // No valid public key
                publicKeyObj = null;
            }
        }
    }

    // Helper method for parsing public key string
    private static byte[] readElement(DataInput dataInput) throws IOException {
        int len = dataInput.readInt();
        byte[] buf = new byte[len];
        dataInput.readFully(buf);
        return buf;
    }
}
