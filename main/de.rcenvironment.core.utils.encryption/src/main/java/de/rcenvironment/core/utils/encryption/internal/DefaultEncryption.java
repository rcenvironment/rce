/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.encryption.internal;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.encryption.EncryptionFactory.EncryptionAlgorithm;
import de.rcenvironment.core.utils.encryption.KeyBasedEncryption;
import de.rcenvironment.core.utils.encryption.PassphraseBasedEncryption;

/**
 * Default implemenation of {@link PassphraseBasedEncryption}.
 * 
 * @author Phillip Rohde
 * @author Sascha Zur
 */
public class DefaultEncryption implements PassphraseBasedEncryption, KeyBasedEncryption {

    protected static final Log LOG = LogFactory.getLog(DefaultEncryption.class);

    protected final EncryptionAlgorithm algorithm;

    public DefaultEncryption(EncryptionAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public String encrypt(String text, String password) {
        return encrypt(text, createKey(password));
    }

    @Override
    public String decrypt(String text, String password) {
        return decrypt(text, createKey(password));
    }

    @Override
    public String encrypt(String text, Key key) {
        String errorMessage = "encrypting text failed";
        try {
            Cipher cipher = Cipher.getInstance(algorithm.getName());
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(text.getBytes());
            byte[] encoded = Base64.encodeBase64(encrypted);
            return StringUtils.newStringUtf8(encoded);
        } catch (NoSuchAlgorithmException e) {
            LOG.error(errorMessage, e);
            return null;
        } catch (NoSuchPaddingException e) {
            LOG.error(errorMessage, e);
            return null;
        } catch (InvalidKeyException e) {
            LOG.error(errorMessage, e);
            return null;
        } catch (IllegalBlockSizeException e) {
            LOG.error(errorMessage, e);
            return null;
        } catch (BadPaddingException e) {
            LOG.error(errorMessage, e);
            return null;
        }
    }

    @Override
    public String decrypt(String text, Key key) {
        String errorMessage = "decrypting text failed";
        try {
            Cipher cipher = Cipher.getInstance(algorithm.getName());
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] crypted = Base64.decodeBase64(text);
            byte[] cipherData = cipher.doFinal(crypted);
            return StringUtils.newString(cipherData, "UTF-8");
        } catch (NoSuchAlgorithmException e) {
            LOG.error(errorMessage, e);
            return null;
        } catch (NoSuchPaddingException e) {
            LOG.error(errorMessage, e);
            return null;
        } catch (InvalidKeyException e) {
            LOG.error(errorMessage, e);
            return null;
        } catch (IllegalBlockSizeException e) {
            LOG.error(errorMessage, e);
            return null;
        } catch (BadPaddingException e) {
            LOG.error(errorMessage, e);
            return null;
        }
    }

    private Key createKey(String password) {
        return new SecretKeySpec(createHashFromKey(password), algorithm.getName());
    }

    private static byte[] createHashFromKey(String passphrase) {
        byte[] key;
        try {
            String salt = "#salty_";
            key = (salt + passphrase).getBytes();
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            final int bits = 16;
            key = Arrays.copyOf(key, bits); // use only first 128 bit
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        return key;

    }

}
