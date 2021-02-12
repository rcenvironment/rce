/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.cluster.internal;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * Holds some test variables.
 * 
 * @author Doreen Seider
 */
public class TestUtils {

    /** Test constants. */
    public int port = 3;

    /** Test constants. */
    public String localHost = RandomStringUtils.random(5);

    /** Test constants. */
    public String localJobId = RandomStringUtils.random(5);

    /** Test constants. */
    public String localSource = RandomStringUtils.random(5);

    /** Test constants. */
    public String remoteHost = RandomStringUtils.random(5);

    /** Test constants. */
    public String remoteJobId = RandomStringUtils.random(5);

    /** Test constants. */
    public String remoteSource = RandomStringUtils.random(5);

}
