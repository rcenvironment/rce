/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.extras.testscriptrunner.internal;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Represents a directory containing golden masters.
 * 
 * @author Alexander Weinert
 */
public class GoldenMasters {

    private Path repositoryPath;

    public GoldenMasters(Path repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    public Optional<GoldenMaster> get(String goldenMasterId) {
        final Path goldenMasterPath = this.repositoryPath.resolve(goldenMasterId + ".json");

        final File goldenMaster = goldenMasterPath.toFile();

        if (!goldenMaster.exists()) {
            return Optional.empty();
        } else {
            return Optional.of(GoldenMaster.fromFile(goldenMaster));
        }
    }

}
