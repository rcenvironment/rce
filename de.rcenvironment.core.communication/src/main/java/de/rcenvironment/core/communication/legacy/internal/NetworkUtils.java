/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.legacy.internal;

import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.Assertions;

/**
 * Utility class for dealing with IP address.
 * 
 * @author Heinrich Wendel
 */
public final class NetworkUtils {

    private static final String ERROR_PARAMETERS_NULL = "The parameter \"%s\" must not be null.";

    /**
     * Regex for IP address.
     */
    private static final String IPV4_REGEX = "[0-9][0-9]?[0-9]?\\.[0-9][0-9]?[0-9]?\\.[0-9][0-9]?[0-9]?\\.[0-9][0-9]?[0-9]?";

    /**
     * Private constructor.
     */
    private NetworkUtils() {}

    /**
     * Checks if the specified host is in the network.
     * 
     * @param hostname The hostname to check.
     * @param network The network, e.g. 192.168.0.0/24.
     * @return true or false.
     */
    public static boolean isHostInNetwork(String hostname, String network) {

        Assertions.isDefined(hostname, StringUtils.format(ERROR_PARAMETERS_NULL, "hostname"));
        Assertions.isDefined(network, StringUtils.format(ERROR_PARAMETERS_NULL, "network"));

        boolean returnCode = true;

        if (hostname.equals(network)) {
            returnCode = true;
        } else if (!hostname.matches(IPV4_REGEX)) {
            returnCode = false;
        } else if (!network.matches(IPV4_REGEX + "/[0-9][0-9]?")) {
            returnCode = false;
        } else {
            String[] splittedNetwork = network.split("/");

            splittedNetwork[1] = convertFromCidrToNetmask(splittedNetwork[1]);

            byte[] addressToTest = addressToByte(hostname);
            byte[] subnet = addressToByte(splittedNetwork[0]);
            byte[] mask = addressToByte(splittedNetwork[1]);

            byte[] addressSubnet = new byte[addressToTest.length];
            for (int i = 0; i < addressToTest.length; i++) {
                addressSubnet[i] = new Integer(addressToTest[i] & mask[i]).byteValue();
            }

            for (int i = 0; i < mask.length; i++) {
                if (subnet[i] != addressSubnet[i]) {
                    returnCode = false;
                    break;
                }
            }
        }

        return returnCode;
    }

    /**
     * Converts an IP address in String format to a byte array.
     * 
     * @param address The address to convert.
     * @return The address in a byte array.
     */
    public static byte[] addressToByte(String address) {

        Assertions.isDefined(address, StringUtils.format(ERROR_PARAMETERS_NULL, "address"));

        if (!address.matches(IPV4_REGEX)) {
            throw new IllegalArgumentException("Wrong format of the address: " + address);
        }

        String[] parts = address.split("\\.");
        byte[] byteAddress = new byte[parts.length];

        for (int i = 0; i < parts.length; i++) {
            byteAddress[i] = new Integer(parts[i]).byteValue();
        }
        return byteAddress;
    }

    /**
     * Converts a CIDR notation to a netmask, e.g. 24 to 255.255.255.0.
     * 
     * @param cidr The CIDR notation.
     * @return return The netmask as String.
     */
    public static String convertFromCidrToNetmask(String cidr) {

        Assertions.isDefined(cidr, StringUtils.format(ERROR_PARAMETERS_NULL, "cidr"));

        Integer cidrInt = null;
        try {
            cidrInt = new Integer(cidr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid cidr notation: " + cidr);
        }

        final int biggest = 32;
        final int smallest = 0;
        if (cidrInt < smallest || cidrInt > biggest) {
            throw new IllegalArgumentException("Invalid cidr notation: " + cidr);
        }

        String[] netmaskParts = new String[4];
        for (int i = 0; i < netmaskParts.length; i++) {
            netmaskParts[i] = "";
        }

        for (int i = 0; i < 4 * 8; i++) {
            if (i < cidrInt) {
                netmaskParts[i / 8] += "1";
            } else {
                netmaskParts[i / 8] += "0";
            }
        }

        String netmask = "";
        for (int i = 0; i < netmaskParts.length; i++) {
            netmask += Integer.parseInt(netmaskParts[i], 2);
            if (i < netmaskParts.length - 1) {
                netmask += ".";
            }
        }

        return netmask;
    }
}
