/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.testing;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public final class StringMatchers {
    private StringMatchers() {}
    
    public static TypeSafeMatcher<String> equalToIgnoringWhiteSpace(final String expectedValue) {
        return new TypeSafeMatcher<String>() {

            @Override
            public void describeTo(Description arg0) {
                arg0.appendText(removeWhitespace(expectedValue));
            }

            @Override
            protected boolean matchesSafely(String arg0) {
                final String actualWithoutWhitespace = removeWhitespace(arg0);
                final String expectedWithoutWhitespace = removeWhitespace(expectedValue);
                return actualWithoutWhitespace.equals(expectedWithoutWhitespace);
            }
            
            private String removeWhitespace(final String string) {
                return string
                    .replace(" ", "")
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("\t", "");
            }
        };
    }
}
