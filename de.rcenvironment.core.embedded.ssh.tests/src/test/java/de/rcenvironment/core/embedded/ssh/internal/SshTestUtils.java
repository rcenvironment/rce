/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.List;

/**
 * Class with utility methods for the SSH console test classes.
 * 
 * @author Sebastian Holtappels
 */
public final class SshTestUtils {

    private static final int DEFAULT_TEST_PORT = 31005;

    private static final String NOT_RESTRICTED_ROLE = SshConstants.ROLE_NAME_DEVELOPER;

    private static final String RESTRICTED_ROLE = SshConstants.ROLE_NAME_LOCAL_ADMIN;

    private static final String PUBLIC_KEY =
        "ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAQEAty3XzVa62Gj6qDpXRiBq+EH72YItRfagVcFedx3+FR7VUzMytZ3BQ/egFu7i"
            + "IK08bptGdSzOgOStykPCYt4594EkTY6zfACguHyUSzqyZoqvpZ534pFC/Nd6i/A7rRcRO1Q7i5xkq0HogkoQTWazCrkwRi"
            + "L0475vnY0qkT6ob8cJe9tTVZYUXuwRjfOPJ52H1c9RdpdCD9NFWwvP0O1ONEdBUST8zyaa/YBaKx2IgArCIIVjB3AkNmsH"
            + "ImnkFWtHhlh/wTp69rlZHPvwUqYD7Nprtk31zHR22ICLvRr/4xL+kPtyZNYkHoe6DnKOS+7WaPczLFAfyMH3TgAyVo0Tbw== imported-openssh-key";

    private SshTestUtils() {};

    /**
     * 
     * Get a valid configuration for test cases.
     * 
     * @return a valid configuration
     */
    public static SshConfiguration getValidConfig() {
        SshConfiguration configuration = new SshConfiguration();
        configuration.setPort(DEFAULT_TEST_PORT);
        configuration.setEnabled(true);

        // add user
        configuration.setStaticAccounts(getValidUsers());

        return configuration;
    }

    /**
     * 
     * Get a list of valid users...
     * 
     * @return a list of valid users
     */
    public static List<SshAccountImpl> getValidUsers() {
        List<SshAccountImpl> users = new ArrayList<SshAccountImpl>();
        users.add(new SshAccountImpl("admin", "adminadmin", null, null, NOT_RESTRICTED_ROLE));
        users.add(new SshAccountImpl("developer", "developerdeveloper", null, null, NOT_RESTRICTED_ROLE));
        users.add(new SshAccountImpl("user", "useruser", null, null, RESTRICTED_ROLE));
        // Testing some special characters and numbers
        users.add(new SshAccountImpl("_1€üöä()!.:,;_-<>[]{}", "_2$1()!.:,;_€üöä-<>[]{}", null, null, RESTRICTED_ROLE));
        users.add(new SshAccountImpl("publicKeyTester", "", null, PUBLIC_KEY, RESTRICTED_ROLE));
        users.add(new SshAccountImpl("default", "default", null, null, RESTRICTED_ROLE));
        return users;
    }

    public static SshAccountImpl getValidUser() {
        return getValidUsers().get(0);
    }

    public static SshAccountImpl getValidPublicKeyUser() {
        return getValidUsers().get(4);
    }
    
    /**
     * Create disabled ssh account.
     * 
     * @return A SshAcountImpl that is disabled.
     */
    public static SshAccountImpl getDisabledUser() {
        SshAccountImpl disabled = new SshAccountImpl("disabled", "disabled", null, null, RESTRICTED_ROLE);
        disabled.setEnabled(false);
        return disabled;
    }
    
    /**
     * Create disabled account with public key.
     * 
     * @return A SshAcountImpl that is disabled.
     */
    public static SshAccountImpl getDisabledPublicKeyUser() {
        SshAccountImpl disabled = new SshAccountImpl("disabled_with_key", "", null, PUBLIC_KEY, RESTRICTED_ROLE);
        disabled.setEnabled(false);
        return disabled;
    }


    /**
     * Creates a valid public key that does not match the key of the "publicKeyTester" account.
     * 
     * @return A public key object
     * 
     * @author Brigitte Boden
     */
    public static PublicKey createIncorrectPublicKey() {
        String mod = "1194457323795445980561452000539327328778638467996523849895883037375273287439705598832111464872863171"
            + "68142202446955508902936035124709397221178664495721428029984726868375359168203283442617134197706515425366188"
            + "396513684446494070223079865755643116690165578452542158755074958452695530623055205290232290667934914919";
        String exp = "42535";
        KeySpec spec = new RSAPublicKeySpec(new BigInteger(mod), new BigInteger(exp));
        KeyFactory kf;
        PublicKey key;
        try {
            kf = KeyFactory.getInstance("RSA");
            key = kf.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            key = null;
        }
        return key;
    }

    public static List<SshAccountRole> getValidRoles() {
        return getValidConfig().getRoles();
    }

}
