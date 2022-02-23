/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.testing;

import org.easymock.Capture;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public final class CaptureMatchers {
    
    private CaptureMatchers() {}

    @SafeVarargs
    public static <T> Matcher<Capture<T>> hasCapturedInAnyOrder(Matcher<T>... args) {
        return new TypeSafeMatcher<Capture<T>>() {

            @Override
            public boolean matchesSafely(Capture<T> capture) {
                for (Matcher<T> arg : args) {
                    final boolean matchingElementFound = capture.getValues().stream()
                        .anyMatch(arg::matches);

                    if (!matchingElementFound) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("has captured ");
                description.appendValueList("[", " and ", "]", args);
                description.appendText(" in any order");
            }

            @Override
            public void describeMismatchSafely(Capture<T> item, Description description) {
                description.appendText("has captured ");
                description.appendValueList("[", ", ", "]", item.getValues());
            }
        };
    }

}
