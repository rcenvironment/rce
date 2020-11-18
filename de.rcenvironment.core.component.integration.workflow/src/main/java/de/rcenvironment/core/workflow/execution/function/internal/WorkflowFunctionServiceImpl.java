/*
 * Copyright 2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.function.internal;

import java.io.File;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.workflow.execution.SynchronousWorkflowExecutionService;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunction;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionService;

@Component
public class WorkflowFunctionServiceImpl implements WorkflowFunctionService {

    private ComponentDataManagementService componentDataManagementService;

    private PlatformService platformService;

    private TypedDatumService typedDatumService;

    private SynchronousWorkflowExecutionService workflowExecutionService;

    private TempFileService tempFileService;

    private Supplier<ObjectWriter> writerSupplier = () -> new ObjectMapper().writer();

    private BiFunction<File, String, File> fileCreator = (parent, child) -> new File(parent, child);

    private Supplier<ObjectMapper> mapperSupplier = () -> new ObjectMapper();

    @Override
    public WorkflowFunction.Builder createBuilder() {
        final FileUtils fileUtils = new FileUtils();
        fileUtils.setCreateObjectWriter(this.writerSupplier);
        fileUtils.setCreateObjectMapper(this.mapperSupplier);
        fileUtils.setCreateFile(this.fileCreator);

        // The "clean" way to do this would be to initialize tempFileService with TempFileServiceAccess.getInstance(). This would, however,
        // access a TempFileService on each instantiation of this class, which is undesirable when unit testing. Thus, we lazily
        // initialize the TempFileService here
        if (this.tempFileService != null) {
            fileUtils.setTempFileService(tempFileService);
        } else {
            fileUtils.setTempFileService(TempFileServiceAccess.getInstance());
        }

        WorkflowFunctionImpl.Builder builder = new WorkflowFunctionImpl.Builder()
            .bindComponentDataManagementService(this.componentDataManagementService)
            .bindPlatformService(this.platformService)
            .bindTypedDatumSerializer(this.typedDatumService.getSerializer())
            .bindWorkflowExecutionService(this.workflowExecutionService)
            .bindFileUtils(fileUtils);
        
        return builder;
        
            
    }

    @Reference
    public void bindComponentDataManagementService(ComponentDataManagementService service) {
        this.componentDataManagementService = service;
    }

    @Reference
    public void bindPlatformService(PlatformService service) {
        this.platformService = service;
    }

    @Reference
    public void bindTypedDatumService(TypedDatumService service) {
        this.typedDatumService = service;
    }

    @Reference
    public void bindSynchronousWorkflowExecutionService(SynchronousWorkflowExecutionService service) {
        this.workflowExecutionService = service;
    }
    
    // This class depends on TempFileService, but that class is not yet able to be injected via OSGi. It is, however, beneficial to be able
    // to inject a mock of this class for testing purposes. Thus, we implement this bind method without decorating it with the @Reference
    // annotation
    public void bindTempFileService(TempFileService tempFileServiceParam) {
        this.tempFileService = tempFileServiceParam;
    }
    
    public void bindObjectWriterSupplier(Supplier<ObjectWriter> writerSupplierParam) {
        this.writerSupplier = writerSupplierParam;
    }
    
    public void bindFileCreator(BiFunction<File, String, File> fileCreatorParam) {
        this.fileCreator = fileCreatorParam;
    }

    public void bindObjectMapperSupplier(Supplier<ObjectMapper> objectMapper) {
        this.mapperSupplier = objectMapper;
        
    }

}
