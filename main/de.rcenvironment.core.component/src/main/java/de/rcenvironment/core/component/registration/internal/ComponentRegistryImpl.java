/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.registration.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.packageadmin.PackageAdmin;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.internal.ComponentBundleConfiguration;
import de.rcenvironment.core.component.model.api.ComponentColor;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentShape;
import de.rcenvironment.core.component.model.api.ComponentSize;
import de.rcenvironment.core.component.model.api.LazyDisposal;
import de.rcenvironment.core.component.model.api.LocalExecutionOnly;
import de.rcenvironment.core.component.model.configuration.impl.ConfigurationDefinitionImpl;
import de.rcenvironment.core.component.model.configuration.impl.ConfigurationExtensionDefinitionImpl;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDefinitionImpl;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDefinitionsProviderImpl;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointGroupDefinitionImpl;
import de.rcenvironment.core.component.model.impl.ComponentInstallationImpl;
import de.rcenvironment.core.component.model.impl.ComponentInterfaceImpl;
import de.rcenvironment.core.component.model.impl.ComponentRevisionImpl;
import de.rcenvironment.core.component.registration.api.ComponentRegistry;
import de.rcenvironment.core.component.registration.api.Registerable;
import de.rcenvironment.core.configuration.CommandLineArguments;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * Implementation of the {@link ComponentRegistry}.
 * 
 * @author Roland Gude
 * @author Jens Ruehmkorf
 * @author Doreen Seider
 * @author Heinrich Wendel
 * @author Robert Mischke
 * @author Sascha Zur
 */
public class ComponentRegistryImpl implements ComponentRegistry {

    private static final int ACTIVATION_TO_COMPONENT_PUBLISHING_DELAY_MSEC = 2000;

    private static final Log LOGGER = LogFactory.getLog(ComponentRegistryImpl.class);

    /**
     * Needed to remove components from {@link ComponentRegistry} which are added/removed by OSGi dependency injection.
     */
    private final Map<String, String> compFactoryServiceIdToCompInstIdMapping = new HashMap<String, String>();

    private final List<ServiceReference<?>> unhandledCompControllers = new ArrayList<ServiceReference<?>>();

    private PlatformService platformService;

    private ConfigurationService configurationService;

    private PackageAdmin packageAdminService;

    private NodeIdentifier localNode;

    private ComponentBundleConfiguration configuration;

    private org.osgi.service.component.ComponentContext osgiComponentCtx;

    private DistributedComponentKnowledgeService componentDistributor;

    private final Map<String, ComponentInstallation> localInstallations = new HashMap<String, ComponentInstallation>();

    private final Map<String, ComponentInstallation> publishedInstallations = new HashMap<String, ComponentInstallation>();

    private volatile boolean publishDelayPast = false;

    private final Object installationLock = new Object();

    // FIXME Currently, tokens, which were not used, are not removed over time-> memory leak, but
    // only minor because of small amount of
    // unused tokens and because of small size of each token. But anyways: token garbage collection
    // must be added -- seid_do, Nov 2013
    // (see: https://www.sistec.dlr.de/mantis/view.php?id=9539)
    private final Set<String> instantiationAuthTokens = Collections.synchronizedSet(new HashSet<String>());

    protected synchronized void activate(org.osgi.service.component.ComponentContext newContext) {

        localNode = platformService.getLocalNodeId();

        synchronized (unhandledCompControllers) {
            osgiComponentCtx = newContext;
            // TODO >6.0.0 rework; bridge code to map the new configuration layout onto the old java bean
            ConfigurationSegment configurationSegment = configurationService.getConfigurationSegment("publishing");
            try {
                // simple migration path for now: mapping the whole of /components onto the old object.
                // this will need to be changed to expand to more configuration settings - misc_ro
                configuration = configurationSegment.mapToObject(ComponentBundleConfiguration.class);
            } catch (IOException e) {
                LOGGER.error("Failed to parse component publish settings; using default values", e);
                configuration = new ComponentBundleConfiguration();
            }
            for (ServiceReference<?> component : unhandledCompControllers) {
                addComponent(component);
            }
            unhandledCompControllers.clear();
        }

        if (!CommandLineArguments.isDoNotStartComponentsRequested()) {
            Runnable r = new Runnable() {

                @Override
                @TaskDescription("Start all bundles providing RCE components")
                public void run() {
                    // start all future Bundles providing a Component
                    osgiComponentCtx.getBundleContext().addBundleListener(new ComponentBundleListener());
                    // start all current Bundles providing a Component
                    for (Bundle b : osgiComponentCtx.getBundleContext().getBundles()) {
                        ComponentBundleListener.handleBundle(b);
                    }
                }
            };
            SharedThreadPool.getInstance().execute(r);
        } else {
            LOGGER.debug("Not triggering start of remaining component bundles as component loading is disabled");
        }

        // start publishing of components after short delay to avoid frequent updates during initial
        // component registration phase
        SharedThreadPool.getInstance().scheduleAfterDelay(new Runnable() {

            @Override
            @TaskDescription("Publish workflow components")
            public void run() {
                synchronized (installationLock) {
                    publishDelayPast = true;
                    publishComponents();
                }
            }

        }, ACTIVATION_TO_COMPONENT_PUBLISHING_DELAY_MSEC);
    }

    protected void bindPlatformService(PlatformService newPlatformService) {
        platformService = newPlatformService;
    }

    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService newInstance) {
        componentDistributor = newInstance;
    }

    protected void bindConfigurationService(ConfigurationService newConfigurationService) {
        configurationService = newConfigurationService;
    }

    protected void bindPackageAdminService(PackageAdmin newPackageAdminService) {
        packageAdminService = newPackageAdminService;
    }

    /**
     * Bind method called by the OSGi framework, if a new Component Factory of type de.rcenvironment.rce.component was registered. The
     * component registration stuff is done here.
     */
    protected synchronized void addComponent(ServiceReference<?> factoryReference) {

        if (CommandLineArguments.isDoNotStartComponentsRequested()) {
            LOGGER.warn("Received a component registration call although components should be disabled: "
                + factoryReference.getBundle().getSymbolicName());
            return;
        }

        // if this bundle is not activated yet, store the component controller and handle it after
        // activation within the activate method
        synchronized (unhandledCompControllers) {
            if (osgiComponentCtx == null) {
                unhandledCompControllers.add(factoryReference);
                return;
            }
        }

        // create and thus, register an instance of the component created by the given factory to
        // access its service properties
        // to be able to find the newly created service, create and thus, register it with an
        // "unique" id as service property
        String compIdentifier = UUID.randomUUID().toString();
        ComponentFactory factory = (ComponentFactory) osgiComponentCtx.getBundleContext().getService(factoryReference);

        ComponentInstance compInstance = createOsgiComponentInstance(factory, compIdentifier);

        if (compInstance != null) {
            try {
                ServiceReference<?> componentReference = getComponentReference(compIdentifier);

                if (componentReference != null) {

                    ComponentRevisionImpl componentRevision = createComponentRevision(componentReference);

                    if (componentRevision != null) {

                        ComponentInstallation componentInstallation = createComponentInstallation(componentRevision);
                        compFactoryServiceIdToCompInstIdMapping.put(factoryReference.getProperty(Constants.SERVICE_ID).toString(),
                            componentInstallation.getInstallationId());
                        addComponent(componentInstallation);
                        return;
                    }
                }

            } finally {
                try {
                    compInstance.dispose();
                } catch (NullPointerException e) {
                    // OSGi complains if no unbind method was declared in ServiceComponent for a service reference. As we cannot be sure,
                    // each
                    // component developer is aware of that, a possible NPE is caught here
                    LOGGER.debug("NPE, most likely cause: unbind method  was not declared: " + e.toString());
                }
            }

        }

        LOGGER.error(StringUtils.format("Failed to register a component, try restarting RCE (affected bundle: %s)",
            factoryReference.getBundle().toString()));
    }

    protected synchronized void removeComponent(ServiceReference<?> factoryReference) {
        String compIdentifier = compFactoryServiceIdToCompInstIdMapping.get(factoryReference.getProperty(Constants.SERVICE_ID).toString());
        removeComponent(compIdentifier);
    }

    private boolean isAllowedToBePublished(ComponentInstallation compInstallation) {
        ComponentInterface componentInterface = compInstallation.getComponentRevision().getComponentInterface();
        if (componentInterface.getLocalExecutionOnly()) {
            return false;
        }
        boolean isPublished = false;
        if (compInstallation.getIsPublished()) {
            isPublished = true;
        } else {
            // Note: quite inefficient, but not called very often
            for (String publishedId : configuration.getPublished()) {
                for (String identifier : componentInterface.getIdentifiers()) {
                    if (identifier.startsWith(publishedId)) {
                        isPublished = true;
                        break;
                    }
                }
            }
        }
        return isPublished;
    }

    private byte[] readIcon(String key, ServiceReference<?> reference) {
        String iconPath = (String) reference.getProperty(key);
        if (iconPath != null) {
            String iconName = (String) reference.getProperty(key);
            String bundleId = "de.rcenvironment.components.";
            if (reference.getProperty(ComponentConstants.COMPONENT_ICON_BUNDLE_NAME_KEY) == null) {
                String componentName = (String) reference.getProperty(ComponentConstants.COMPONENT_NAME_KEY);
                bundleId += (componentName.toLowerCase() + ".common");
            } else {
                bundleId = (String) reference.getProperty(ComponentConstants.COMPONENT_ICON_BUNDLE_NAME_KEY);
            }
            URL url = ComponentUtils.readIconURL(bundleId, iconName);
            if (url != null) {
                try (InputStream stream = url.openStream();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    while (true) {
                        int r = stream.read();
                        if (r < 0) {
                            break;
                        }
                        bos.write(r);
                    }
                    return bos.toByteArray();
                } catch (IOException e) {
                    LOGGER.warn("Failed to read icon: " + iconPath);
                    return null;
                }
            } else {
                LOGGER.warn("Icon not found: " + iconPath);
            }
        }
        return null;

    }

    @Override
    public void addComponent(ComponentInstallation componentInstallation) {
        synchronized (installationLock) {
            localInstallations.put(componentInstallation.getInstallationId(), componentInstallation);
            if (isAllowedToBePublished(componentInstallation)) {
                publishedInstallations.put(componentInstallation.getInstallationId(), componentInstallation);
            }
            publishComponents();
        }
        LOGGER.debug("Registered component: " + componentInstallation.getComponentRevision().getComponentInterface().getIdentifier());
    }

    @Override
    public void removeComponent(String compInstallationId) {
        synchronized (installationLock) {
            localInstallations.remove(compInstallationId);
            publishedInstallations.remove(compInstallationId);
            publishComponents();
        }
        LOGGER.debug("Removed component: " + compInstallationId);
    }

    @Override
    public void addComponentInstantiationAuthToken(String token) {
        if (token != null) {
            instantiationAuthTokens.add(token);
        }
    }

    private void publishComponents() {
        if (publishDelayPast) {
            componentDistributor.setLocalComponentInstallations(localInstallations.values(), publishedInstallations.values());
        } else {
            componentDistributor.setLocalComponentInstallations(localInstallations.values(), new ArrayList<ComponentInstallation>());
        }
    }

    private ComponentInterfaceImpl configureComponentInterfaceFromImplementingClass(ComponentInterfaceImpl componentInterface,
        String className) {

        Class<?> componentClass;
        try {
            componentClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Failed to load component class: " + className, e);
            return null;
        }

        componentInterface.setLocalExecutionOnly(componentClass.getAnnotation(LocalExecutionOnly.class) != null);
        componentInterface.setPerformLazyDisposal(componentClass.getAnnotation(LazyDisposal.class) != null);
        componentInterface.setIsDeprecated(componentClass.getAnnotation(
            de.rcenvironment.core.component.model.api.Deprecated.class) != null);

        return componentInterface;
    }

    private ComponentInstance createOsgiComponentInstance(ComponentFactory factory, String identifier) {

        Dictionary<String, String> serviceProperties = new Hashtable<String, String>();
        serviceProperties.put(ComponentConstants.COMP_INSTANCE_ID_KEY, identifier);

        try {
            return factory.newInstance(serviceProperties);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to load component because of an error in the OSGi DS file or an error in the component's constructor", e);
            return null;
        }
    }

    private ServiceReference<?> getComponentReference(String identifier) {

        // search for the previously registered service (component)
        String filter = "(" + ComponentConstants.COMP_INSTANCE_ID_KEY + "=" + identifier + ")";

        ServiceReference<?>[] references;
        try {
            references = osgiComponentCtx.getBundleContext().getAllServiceReferences(Registerable.class.getName(), filter);

            if (references == null || references.length == 0) {
                LOGGER.error(StringUtils.format(
                    "No component found that provides the service '%s' and that has the temporary identifier '%s'",
                    Registerable.class.getName(), identifier));
                return null;
            } else if (references.length > 1) {
                LOGGER.warn(StringUtils.format(
                    "More than one component found that provides the service '%s' and that has the temporary identifier '%s',"
                        + " first one is taken",
                    Registerable.class.getName(), identifier));
            }

        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException("Invalid syntax. This is a bug.", e);
        }

        return references[0];
    }

    private ComponentInstallation createComponentInstallation(ComponentRevisionImpl componentRevision) {
        ComponentInstallationImpl componentInstallation = new ComponentInstallationImpl();
        // must be changed in future to be unique per node
        componentInstallation.setInstallationId(componentRevision.getComponentInterface().getIdentifier());
        componentInstallation.setComponentRevision(componentRevision);
        componentInstallation.setNodeId(localNode.getIdString());
        return componentInstallation;
    }

    private void setIdentifiers(ServiceReference<?> componentReference, ComponentInterfaceImpl componentInterface) {
        String identifier;
        List<String> identifiers = null;

        // use explicitly given id(s) if present, otherwise generate id the "old" way.
        Object identifierObject = componentReference.getProperty(ComponentConstants.COMPONENT_ID_KEY);
        if (identifierObject != null) {
            if (identifierObject instanceof String[]) {
                String[] identifiersArray = (String[]) identifierObject;
                if (identifiersArray.length == 0) {
                    throw new IllegalArgumentException(
                        "At least one component identifier must be defined whithin in XML definition file of the component");
                }
                identifier = identifiersArray[0];
                identifiers = Arrays.asList(identifiersArray);
            } else {
                identifier = (String) identifierObject;
            }
        } else {
            String name = (String) componentReference.getProperty(ComponentConstants.COMPONENT_NAME_KEY);
            identifier = (String) componentReference.getProperty(ComponentConstants.COMPONENT_CLASS_KEY)
                + ComponentConstants.COMPONENT_ID_SEPARATOR + name;
        }

        if (identifiers == null) {
            identifiers = new ArrayList<>();
            identifiers.add(identifier);
        }

        componentInterface.setIdentifier(identifier);
        componentInterface.setIdentifiers(identifiers);
    }

    private ComponentRevisionImpl createComponentRevision(ServiceReference<?> componentReference) {

        ComponentInterfaceImpl componentInterface = new ComponentInterfaceImpl();
        ComponentRevisionImpl componentRevision = new ComponentRevisionImpl();

        // read and process all the service properties. actually, they are the properties of the
        // component
        setIdentifiers(componentReference, componentInterface);
        componentInterface.setDisplayName((String) componentReference.getProperty(ComponentConstants.COMPONENT_NAME_KEY));
        componentInterface.setGroupName((String) componentReference.getProperty(ComponentConstants.COMPONENT_NAME_GROUP));
        componentInterface.setVersion((String) componentReference.getProperty(ComponentConstants.VERSION_DEF_KEY));
        componentInterface.setIcon16(readIcon(ComponentConstants.ICON_16_KEY, componentReference));
        componentInterface.setIcon24(readIcon(ComponentConstants.ICON_24_KEY, componentReference));
        componentInterface.setIcon32(readIcon(ComponentConstants.ICON_32_KEY, componentReference));
        componentInterface.setSize(getComponentSize(componentReference));
        componentInterface.setColor(getComponentColor(componentReference));
        componentInterface.setShape(getComponentShape(componentReference));
        componentInterface.setCanHandleNotAValueDataTypes(getCanHandleNotAValueDataTypes(componentReference));
        componentInterface.setIsLoopDriver(getIsResetSink(componentReference));

        try {
            componentInterface.setInputDefinitionsProvider(createEndpointDefinitionsProvider(ComponentConstants.INPUTS_DEF_KEY,
                componentReference, componentReference.getBundle(), EndpointType.INPUT));
        } catch (IOException e) {
            LOGGER.error("Failed to parse input definition", e);
            return null;
        }

        try {
            componentInterface.setOutputDefinitionsProvider(createEndpointDefinitionsProvider(ComponentConstants.OUTPUTS_DEF_KEY,
                componentReference, componentReference.getBundle(), EndpointType.OUTPUT));
        } catch (IOException e) {
            LOGGER.error("Failed to parse output definition", e);
            return null;
        }

        try {
            componentInterface.setConfigurationDefinition(createConfigurationDefinition(ComponentConstants.CONFIGURATION_DEF_KEY,
                componentReference));
        } catch (IOException e) {
            LOGGER.error("Failed to parse configuration definition", e);
            return null;
        }

        try {
            componentInterface.setConfigurationExtensionDefinitions(
                createConfigurationExtensionDefinitions(componentReference.getBundle()));
        } catch (IOException e) {
            LOGGER.error("Failed to parse extension definition", e);
            return null;
        }

        String className = (String) componentReference.getProperty(ComponentConstants.COMPONENT_CLASS_KEY);
        configureComponentInterfaceFromImplementingClass(componentInterface, className);

        componentRevision.setComponentInterface(componentInterface);
        componentRevision.setClassName(className);

        return componentRevision;
    }

    private ComponentColor getComponentColor(ServiceReference<?> componentReference) {
        String color = (String) componentReference.getProperty(ComponentConstants.COMPONENT_COLOR_KEY);
        if (color != null) {
            try {
                return ComponentColor.valueOf(color.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.error(StringUtils.format("Color declared under '%s' is invalid: %s. Valid ones are: %s. Default will be used: %s",
                    ComponentConstants.COMPONENT_COLOR_KEY, color, Arrays.toString(ComponentColor.values()),
                    ComponentConstants.COMPONENT_COLOR_STANDARD));
            }
        }
        return ComponentConstants.COMPONENT_COLOR_STANDARD;
    }

    private boolean getCanHandleNotAValueDataTypes(ServiceReference<?> componentReference) {
        return Boolean.valueOf((String) componentReference.getProperty(ComponentConstants.COMPONENT_CAN_HANDLE_NAV_INPUT_DATA_TYPES));
    }

    private boolean getIsResetSink(ServiceReference<?> componentReference) {
        return Boolean.valueOf((String) componentReference.getProperty(LoopComponentConstants.COMPONENT_IS_RESET_SINK));
    }

    private ComponentSize getComponentSize(ServiceReference<?> componentReference) {
        String size = (String) componentReference.getProperty(ComponentConstants.COMPONENT_SIZE_KEY);
        if (size != null) {
            try {
                return ComponentSize.valueOf(size.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.error(StringUtils.format("Size declared under '%s' is not valid: %s. Valid ones are: %s. Default will be used: %s",
                    ComponentConstants.COMPONENT_SIZE_KEY, size, Arrays.toString(ComponentSize.values()),
                    ComponentConstants.COMPONENT_SIZE_STANDARD));
            }
        }
        return ComponentConstants.COMPONENT_SIZE_STANDARD;
    }

    private ComponentShape getComponentShape(ServiceReference<?> componentReference) {
        String shape = (String) componentReference.getProperty(ComponentConstants.COMPONENT_SHAPE_KEY);
        if (shape != null) {
            try {
                return ComponentShape.valueOf(shape.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.error(StringUtils.format("Shape declared under '%s' is not valid: %s. Valid ones are: %s. Default will be used: %s",
                    ComponentConstants.COMPONENT_SHAPE_KEY, shape, Arrays.toString(ComponentShape.values()),
                    ComponentConstants.COMPONENT_SHAPE_STANDARD));
            }
        }
        return ComponentConstants.COMPONENT_SHAPE_STANDARD;
    }

    private EndpointDefinitionsProviderImpl createEndpointDefinitionsProvider(String key, ServiceReference<?> reference,
        Bundle componentsBundle, EndpointType type) throws IOException {

        String file = (String) reference.getProperty(key);
        if (file == null) {
            return new EndpointDefinitionsProviderImpl();
        }
        URL fileUrl = reference.getBundle().getResource(file);
        if (fileUrl == null) {
            throw new IOException("Endpoint definition file doesn't exist: " + file);
        }

        List<InputStream> extendedStaticInputStreams = getInputStreamsForEndpointMetaDataExtensions(reference, componentsBundle, type);
        List<InputStream> extendedDynamicInputStreams = getInputStreamsForEndpointMetaDataExtensions(reference, componentsBundle, type);

        EndpointDefinitionsProviderImpl endpointProvider = new EndpointDefinitionsProviderImpl();

        Set<EndpointDefinitionImpl> endpointDefinitions;
        try (InputStream staticEndpointDescriptionInputStream = fileUrl.openStream();
            InputStream dynamicEndpointDescriptionInputStream = fileUrl.openStream()) {
            endpointDefinitions = ComponentUtils.extractStaticEndpointDefinition(staticEndpointDescriptionInputStream,
                extendedStaticInputStreams, type);
            endpointDefinitions.addAll(ComponentUtils.extractDynamicEndpointDefinition(dynamicEndpointDescriptionInputStream,
                extendedDynamicInputStreams, type));
        } catch (IOException e) {
            throw new IOException("Failed to parse endpoint definition file: " + file, e);
        }

        endpointProvider.setEndpointDefinitions(endpointDefinitions);

        Set<EndpointGroupDefinitionImpl> inputGroupDefinitions;
        try (InputStream endpointGroupDefinitionInputStream = fileUrl.openStream();
            InputStream staticEndpointGroupDefinitionInputStream = fileUrl.openStream();
            InputStream dynamicEndpointGroupDefinitionInputStream = fileUrl.openStream()) {
            inputGroupDefinitions = ComponentUtils.extractStaticInputGroupDefinitions(staticEndpointGroupDefinitionInputStream);
            inputGroupDefinitions.addAll(ComponentUtils.extractDynamicInputGroupDefinitions(dynamicEndpointGroupDefinitionInputStream));
        } catch (IOException e) {
            throw new IOException("Failed to parse endpoint group definition file: " + file, e);
        }

        endpointProvider.setEndpointGroupDefinitions(inputGroupDefinitions);
        return endpointProvider;
    }

    private List<InputStream> getInputStreamsForEndpointMetaDataExtensions(ServiceReference<?> reference, Bundle componentsBundle,
        EndpointType type) throws IOException {
        List<InputStream> inputStreams = new ArrayList<>();

        String manifestHeaderKey;
        if (type == EndpointType.INPUT) {
            manifestHeaderKey = ComponentConstants.MANIFEST_ENTRY_RCE_COMPONENT_EXTENSION_INPUT_META_DATA;
        } else {
            manifestHeaderKey = ComponentConstants.MANIFEST_ENTRY_RCE_COMPONENT_EXTENSION_OUTPUT_META_DATA;
        }
        for (Bundle fragment : getFragmentBundlesProvidingPropertyExtensions(componentsBundle)) {
            Dictionary<String, String> manifestHeaders = fragment.getHeaders();
            String extensionFile = manifestHeaders.get(manifestHeaderKey);
            if (extensionFile != null) {
                URL fileUrl = reference.getBundle().getResource(extensionFile);
                if (fileUrl == null) {
                    throw new IOException("Endpoint definition file doesn't exist: " + extensionFile);
                }
                inputStreams.add(fileUrl.openStream());
            }
        }
        return inputStreams;
    }

    private ConfigurationDefinitionImpl createConfigurationDefinition(String key, ServiceReference<?> reference)
        throws IOException {

        String file = (String) reference.getProperty(key);
        if (file == null) {
            return new ConfigurationDefinitionImpl();
        }
        URL fileUrl = reference.getBundle().getResource(file);
        if (fileUrl == null) {
            throw new IOException("Configuration definition file doesn't exist: " + file);
        }

        try (InputStream configurationDescriptionInputStream = fileUrl.openStream();
            InputStream placeholdersDescriptionInputStream = fileUrl.openStream();
            InputStream activationFilterDescriptionInputStream = fileUrl.openStream()) {
            return ComponentUtils.extractConfigurationDescription(configurationDescriptionInputStream, placeholdersDescriptionInputStream,
                activationFilterDescriptionInputStream);
        } catch (IOException e) {
            throw new IOException("Failed to parse configuration definition", e);
        }
    }

    private Set<ConfigurationExtensionDefinitionImpl> createConfigurationExtensionDefinitions(Bundle componentsBundle)
        throws IOException {

        Set<ConfigurationExtensionDefinitionImpl> descs = new HashSet<ConfigurationExtensionDefinitionImpl>();

        for (Bundle fragment : getFragmentBundlesProvidingPropertyExtensions(componentsBundle)) {
            Dictionary<String, String> manifestHeaders = fragment.getHeaders();
            String extConfigFile = manifestHeaders.get(ComponentConstants.MANIFEST_ENTRY_RCE_COMPONENT_EXTENSION_CONFIGURATION);
            if (extConfigFile != null) {
                descs.add(createConfigurationExtensionDefinition(componentsBundle, extConfigFile));
            }
        }
        return descs;
    }

    private ConfigurationExtensionDefinitionImpl createConfigurationExtensionDefinition(Bundle bundle, String file)
        throws IOException {

        URL fileUrl = bundle.getResource(file);
        if (fileUrl == null) {
            throw new IOException("Configuration extension file doesn't exist: " + file);
        }

        try (InputStream configurationDescriptionInputStream = fileUrl.openStream();
            InputStream placeholdersDescriptionInputStream = fileUrl.openStream();
            InputStream activationFilterDescriptionInputStream = fileUrl.openStream();) {
            return ComponentUtils.extractConfigurationExtensionDescription(
                configurationDescriptionInputStream,
                placeholdersDescriptionInputStream,
                activationFilterDescriptionInputStream);
        } catch (IOException e) {
            throw new IOException("Failed to parse configuration extension definition file: " + file, e);
        }
    }

    private Bundle[] getFragmentBundlesProvidingPropertyExtensions(Bundle componentsBundle) {

        Bundle[] fragments = packageAdminService.getFragments(componentsBundle);
        if (fragments == null) {
            fragments = new Bundle[0];
        }
        return fragments;
    }

    /**
     * For test purposes only.
     */
    protected void setOsgiComponentContext(org.osgi.service.component.ComponentContext compContext) {
        this.osgiComponentCtx = compContext;
    }

    /**
     * For test pruposes only.
     */
    protected void setComponentBundleConfiguration(ComponentBundleConfiguration config) {
        this.configuration = config;
    }

}
