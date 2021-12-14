/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.extras.testscriptrunner.internal;

import java.io.File;

/**
 * A golden master is a serialized description of a single workflow execution. For the origin of the name, please refer to
 * https://en.wikipedia.org/wiki/Characterization_test.
 * 
 * @author Alexander Weinert
 */
public class GoldenMaster {

    private final File master;

    public GoldenMaster(File goldenMaster) {
        this.master = goldenMaster;
    }

    public static GoldenMaster fromFile(File goldenMaster) {
        return new GoldenMaster(goldenMaster);
    }

    public String getAbsolutePath() {
        return this.master.getAbsolutePath();
    }

    public String getWorkflowName() {
        return this.master.getName().substring(0, this.master.getName().length() - ".json".length());
    }

}
