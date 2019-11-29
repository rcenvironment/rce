/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.integration;

import org.easymock.EasyMock;

/**
 * Builder class for a mocked {@link ToolIntegrationContext}.
 * 
 * @author Alexander Weinert
 */
public class MockToolIntegrationContextBuilder {

    private String rootPathToToolIntegrationDirectory;

    private String nameOfToolIntegrationDirectory;

    private String contextType;

    private String configurationFilename;

    /**
     * @param rootPathToToolIntegrationDirectoryParam The desired return of a call to .getRootPathToToolIntegrationDirectoryParam.
     * @return This builder-object for daisy-chaining.
     */
    public MockToolIntegrationContextBuilder rootPathToToolIntegrationDirectory(String rootPathToToolIntegrationDirectoryParam) {
        rootPathToToolIntegrationDirectory = rootPathToToolIntegrationDirectoryParam;
        return this;
    }

    /**
     * @param nameOfToolIntegrationDirectoryParam The desired return of a call to .getNameOfToolIntegrationDirectory()
     * @return This builder-object for daisy-chaining.
     */
    public MockToolIntegrationContextBuilder nameOfToolIntegrationDirectory(String nameOfToolIntegrationDirectoryParam) {
        nameOfToolIntegrationDirectory = nameOfToolIntegrationDirectoryParam;
        return this;
    }
    
    /**
     * @param contextTypeParam The desired return of a call to .getContextType()
     * @return This builder-object for daisy-chaining.
     */
    public MockToolIntegrationContextBuilder contextType(String contextTypeParam) {
        contextType = contextTypeParam;
        return this;
    }

    /**
     * @param configurationFilenameParam The desired return of a call to .getConfigurationFilename()
     * @return This builder-object for daisy-chaining.
     */
    public MockToolIntegrationContextBuilder configurationFilename(String configurationFilenameParam) {
        configurationFilename = configurationFilenameParam;
        return this;
    }

    /**
     * @return A mocked ToolIntegrationContext that behaves as configured by previous calls to this object.
     */
    public ToolIntegrationContext build() {
        final ToolIntegrationContext context = EasyMock.createMock(ToolIntegrationContext.class);

        if (rootPathToToolIntegrationDirectory != null) {
            EasyMock.expect(context.getRootPathToToolIntegrationDirectory()).andStubReturn(rootPathToToolIntegrationDirectory);
        }

        if (nameOfToolIntegrationDirectory != null) {
            EasyMock.expect(context.getNameOfToolIntegrationDirectory()).andStubReturn(nameOfToolIntegrationDirectory);
        }
        
        if (contextType != null) {
            EasyMock.expect(context.getContextType()).andStubReturn(contextType);
        }

        if (configurationFilename != null) {
            EasyMock.expect(context.getConfigurationFilename()).andStubReturn(configurationFilename);
        }

        EasyMock.replay(context);
        return context;
    }

}
