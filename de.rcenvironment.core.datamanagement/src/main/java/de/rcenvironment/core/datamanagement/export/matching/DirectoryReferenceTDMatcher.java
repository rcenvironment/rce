/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.export.matching;

import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;

/**
 * Matches two directory references.
 * 
 * @author Marlon Schroeter
 *
 */
public class DirectoryReferenceTDMatcher implements Matcher<DirectoryReferenceTD> {

    @Override
    public MatchResult matches(DirectoryReferenceTD actual, DirectoryReferenceTD expected) {
        MatchResult result = new MatchResult();

        if (!actual.getDirectoryReference().equals(expected.getDirectoryReference())) {
            result.addFailureCause("The directory reference values are not the same.");
        }

        return result;
    }

}
