/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.excel.common;

import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Class providing convenient access to the Excel Service.
 * <p>
 * (Note: This class was added to replace the original SimpleExcelService, which was a member of a general delegate concept. I replaced it
 * with this small access class instead of rewriting each caller, as the service is fetched in quite a few places.-- misc_ro, Jan 2019)
 *
 * @author Robert Mischke
 */
public final class ExcelServiceAccess {

    private static final ExcelServiceAccess INSTANCE = new ExcelServiceAccess();

    private final ExcelService excelService;

    private ExcelServiceAccess() {
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        excelService = serviceRegistryAccess.getService(ExcelService.class);
    }

    /**
     * @return the global {@link ExcelService} instance
     */
    public static ExcelService get() {
        return INSTANCE.excelService;
    }
}
