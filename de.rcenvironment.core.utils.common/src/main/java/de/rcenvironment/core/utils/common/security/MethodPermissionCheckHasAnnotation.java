/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.security;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * A {@link MethodPermissionCheck} that verifies the presence of an annotation on the target method.
 * 
 * Note that annotations are only inherited from methods in parent <b>classes</b>, but <b>not</b>
 * from methods in implemented <b>interfaces</b>.
 * 
 * @author Robert Mischke
 */
public final class MethodPermissionCheckHasAnnotation implements MethodPermissionCheck {

    private Class<? extends Annotation> annotationClass;

    public MethodPermissionCheckHasAnnotation(Class<? extends Annotation> annotationClass) {
        this.annotationClass = annotationClass;
    }

    @Override
    public boolean checkPermission(Method method) {
        return method.isAnnotationPresent(annotationClass);
    }
}
