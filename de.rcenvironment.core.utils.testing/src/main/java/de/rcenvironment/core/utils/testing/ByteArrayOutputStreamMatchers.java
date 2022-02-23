/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.testing;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public final class ByteArrayOutputStreamMatchers {
    private ByteArrayOutputStreamMatchers() {}

    public static Matcher<ByteArrayOutputStream> isUTF8StringThat(Matcher<String> matcher) {
        return new TypeSafeMatcher<ByteArrayOutputStream>() {
            @Override
            public void describeTo(Description arg0) {
                arg0.appendText("is a UTF8-encoded string that ");
                matcher.describeTo(arg0);
            }

            @Override
            protected boolean matchesSafely(ByteArrayOutputStream arg0) {
                final String string = new String(arg0.toByteArray(), StandardCharsets.UTF_8);
                return matcher.matches(string);
            }
        };
    }
}
