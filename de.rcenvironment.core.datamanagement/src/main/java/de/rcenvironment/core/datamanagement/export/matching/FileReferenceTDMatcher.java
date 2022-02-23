/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.export.matching;

import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;

/**
 * Matches two file references.
 * 
 * @author Marlon Schroeter
 *
 */
public class FileReferenceTDMatcher implements Matcher<FileReferenceTD> {

    @Override
    public MatchResult matches(FileReferenceTD actual, FileReferenceTD expected) {
        MatchResult result = new MatchResult();

        if (!actual.getFileReference().equals(expected.getFileReference())) {
            result.addFailureCause("The directory reference values are not the same.");
        }

        return result;
    }

}
