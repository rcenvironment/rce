/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.toolkitbridge.transitional;

import de.rcenvironment.core.toolkitbridge.api.StaticToolkitHolder;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.ConcurrencyUtilsFactory;
import de.rcenvironment.toolkit.modules.concurrency.api.ThreadPoolManagementAccess;

/**
 * A transitional class providing singleton access to the redesigned concurrency and AsyncTask services, until these are being used as
 * native services throughout the code base.
 * 
 * @author Robert Mischke
 */
public final class ConcurrencyUtils {

    static final AsyncTaskService SHARED_THREAD_POOL_INSTANCE = StaticToolkitHolder
        .getServiceWithUnitTestFallback(AsyncTaskService.class);

    static final ThreadPoolManagementAccess SHARED_THREAD_POOL_MANAGEMENT_ACCESS = StaticToolkitHolder
        .getServiceWithUnitTestFallback(ThreadPoolManagementAccess.class);

    private static final ConcurrencyUtilsFactory SHARED_FACTORY_INSTANCE = StaticToolkitHolder
        .getServiceWithUnitTestFallback(ConcurrencyUtilsFactory.class);

    private ConcurrencyUtils() {}

    public static AsyncTaskService getAsyncTaskService() {
        return SHARED_THREAD_POOL_INSTANCE;
    }

    /**
     * Transitional method for access to thread pool management calls.
     * 
     * @return the {@link ThreadPoolManagementAccess} instance
     */
    public static ThreadPoolManagementAccess getThreadPoolManagement() {
        return SHARED_THREAD_POOL_MANAGEMENT_ACCESS;
    }

    public static ConcurrencyUtilsFactory getFactory() {
        return SHARED_FACTORY_INSTANCE;
    }

}
