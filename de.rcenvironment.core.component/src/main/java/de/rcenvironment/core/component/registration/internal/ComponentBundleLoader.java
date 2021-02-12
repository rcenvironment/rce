/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.packageadmin.PackageAdmin;

import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.UserComponentIdMappingService;
import de.rcenvironment.core.component.internal.ComponentBundleConfiguration;
import de.rcenvironment.core.component.management.api.LocalComponentRegistrationService;
import de.rcenvironment.core.component.model.api.ComponentColor;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
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
import de.rcenvironment.core.component.registration.api.Registerable;
import de.rcenvironment.core.configuration.CommandLineArguments;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Loads predefined components (those defined by Java bundles instead of user configuration), and registers them at the
 * {@link LocalComponentRegistrationService}.
 * 
 * Note: The concept of component registration should be replaced with one that doesn't require a temporary OSGi service registration. It is
 * just not needed anymore (components are no OSGi services anymore) and on the other side, it is kind of complicated to temporary register
 * an OSGi service just to fetch its properties. The properties instead should not be stored as OSGi service properties anymore but e.g.
 * next to the configuration.json, inputs.json, etc. The main advantage of the current approach: Components just get injected to this class
 * here with the help of OSGi DS and certain bind method. I could imagine to keep this benefit by just registering an OSGi service per
 * component that provides all of the information belonging to a component and keeps registered until the component's bundle got
 * removed.<br>
 * There is also a specific issue regarding the registration: https://mantis.sc.dlr.de/view.php?id=12916 --seid_do
 * 
 * @author Roland Gude
 * @author Jens Ruehmkorf
 * @author Doreen Seider
 * @author Heinrich Wendel
 * @author Robert Mischke
 * @author Sascha Zur
 */
@Component(immediate = true)
public class ComponentBundleLoader {

    /**
     * Needed to remove components from {@link LocalComponentRegistrationService} which are added/removed by OSGi dependency injection.
     */
    private final Map<String, String> compFactoryServiceIdToCompInstIdMapping = new HashMap<String, String>();

    private final List<ServiceReference<?>> unhandledCompControllers = new ArrayList<ServiceReference<?>>();

    private PlatformService platformService;

    private ConfigurationService configurationService;

    private LocalComponentRegistrationService componentRegistrationService;

    private UserComponentIdMappingService userComponentIdMappingService;

    private PackageAdmin packageAdminService;

    private LogicalNodeId localDefaultLogicalNodeId;

    private ComponentBundleConfiguration configuration;

    private org.osgi.service.component.ComponentContext osgiComponentCtx;

    // FIXME Currently, unused tokens are not removed over time -> memory leak, but only minor because of small amount of
    // unused tokens and because of small size of each token. But anyway: token garbage collection must be added -- seid_do, Nov 2013 (see:
    // https://www.sistec.dlr.de/mantis/view.php?id=9539)
    @Deprecated // TODO (p2) unused; check for removal in 9.0
    private final Set<String> instantiationAuthTokens = Collections.synchronizedSet(new HashSet<String>());

    private AuthorizationService authorizationService;

    private final Log log = LogFactory.getLog(getClass());

    @Activate
    protected synchronized void activate(org.osgi.service.component.ComponentContext newContext) {

        localDefaultLogicalNodeId = platformService.getLocalDefaultLogicalNodeId();

        // load (legacy) JSON publication configuration
        // TODO >6.0.0 rework; bridge code to map the new configuration layout onto the old java bean
        ConfigurationSegment configurationSegment = configurationService.getConfigurationSegment("publishing");
        try {
            // simple migration path for now: mapping the whole of /components onto the old object.
            // this will need to be changed to expand to more configuration settings - misc_ro
            configuration = configurationSegment.mapToObject(ComponentBundleConfiguration.class);
        } catch (IOException e) {
            log.error("Failed to parse component publish settings; using default values", e);
            configuration = new ComponentBundleConfiguration();
        }

        // log warnings for legacy publication entries
        for (String publishedId : configuration.getPublished()) {
            log.warn("Found a deprecated publication entry \"" + publishedId
                + "\" in the profile's configuration file, but it will not be applied; "
                + "use the component authorization system to publish components");
        }

        // register component declarations that were held back because they activated before this service
        synchronized (unhandledCompControllers) {
            osgiComponentCtx = newContext;
            for (ServiceReference<?> component : unhandledCompControllers) {
                registerXmlComponentDeclaration(component);
            }
            unhandledCompControllers.clear();
        }

        if (!CommandLineArguments.isDoNotStartComponentsRequested()) {
            ConcurrencyUtils.getAsyncTaskService().execute("Start all bundles providing RCE components", () -> {
                // start all future Bundles providing a Component
                osgiComponentCtx.getBundleContext().addBundleListener(new ComponentBundleListener());
                // start all current Bundles providing a Component
                for (Bundle b : osgiComponentCtx.getBundleContext().getBundles()) {
                    ComponentBundleListener.handleBundle(b);
                }

                // signal to the local component registration service that bundle init is complete
                componentRegistrationService.reportBuiltinComponentLoadingComplete();
            });
        } else {
            log.debug("Not triggering start of remaining component bundles as component loading is disabled");
            // signal to the local component registration service that bundle initialization is complete
            // even if components loading is disabled
            componentRegistrationService.reportBuiltinComponentLoadingComplete();
        }

    }

    @Reference
    protected void bindPlatformService(PlatformService newPlatformService) {
        platformService = newPlatformService;
    }

    @Reference
    protected void bindConfigurationService(ConfigurationService newInstance) {
        configurationService = newInstance;
    }

    @Reference
    protected void bindUserComponentIdMappingService(UserComponentIdMappingService newInstance) {
        this.userComponentIdMappingService = newInstance;
    }

    @Reference
    protected void bindComponentRegistrationService(LocalComponentRegistrationService newInstance) {
        this.componentRegistrationService = newInstance;
    }

    @Reference
    protected void bindComponentAuthorizationService(AuthorizationService newInstance) {
        this.authorizationService = newInstance;
    }

    @Reference
    protected void bindPackageAdminService(PackageAdmin newPackageAdminService) {
        packageAdminService = newPackageAdminService;
    }

    /**
     * Bind method called by the OSGi framework, if a new Component Factory of type de.rcenvironment.rce.component was registered. The
     * component registration stuff is done here.
     * 
     * Note that this factory-based approach is legacy code, and is intended to be replaced. In my opinion, it is far too complicated for
     * what functionality it provides. -- misc_ro
     */
    @Reference(service = org.osgi.service.component.ComponentFactory.class, // forced line break
        target = "(component.factory=de.rcenvironment.rce.component)", cardinality = ReferenceCardinality.MULTIPLE, // forced line break
        policy = ReferencePolicy.DYNAMIC, name = "Component Factory", unbind = "unregisterXmlComponentDeclaration")
    protected synchronized void registerXmlComponentDeclaration(ServiceReference<?> factoryReference) {

        if (CommandLineArguments.isDoNotStartComponentsRequested()) {
            log.warn("Received a component registration call although components should be disabled: "
                + factoryReference.getBundle().getSymbolicName());
            return;
        }

        // if this bundle is not activated yet, store the component controller
        // and handle it after
        // activation within the activate method
        synchronized (unhandledCompControllers) {
            if (osgiComponentCtx == null) {
                unhandledCompControllers.add(factoryReference);
                return;
            }
        }

        // create and thus, register an instance of the component created by the
        // given factory to
        // access its service properties
        // to be able to find the newly created service, create and thus,
        // register it with an
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
                        compFactoryServiceIdToCompInstIdMapping.put(
                            factoryReference.getProperty(Constants.SERVICE_ID).toString(), componentInstallation.getInstallationId());

                        // note: this must be done before registering the component, as getComponentSelector() needs the mapping
                        userComponentIdMappingService.registerBuiltinComponentMapping(
                            componentInstallation.getComponentInterface().getIdentifier(),
                            componentInstallation.getComponentInterface().getDisplayName());

                        componentRegistrationService.registerOrUpdateLocalComponentInstallation(componentInstallation);
                        return;
                    }
                }

            } finally {
                try {
                    compInstance.dispose();
                } catch (NullPointerException e) {
                    // OSGi complains if no unbind method is declared in the ServiceComponent for a service reference.
                    // As we cannot be sure that every component developer is aware of that, a possible NPE is caught here
                    log.debug("NPE, most likely cause: unbind method  was not declared: " + e.toString());
                }
            }

        }

        log.error(StringUtils.format("Failed to register a component, try restarting RCE (affected bundle: %s)",
            factoryReference.getBundle().toString()));
    }

    protected synchronized void unregisterXmlComponentDeclaration(ServiceReference<?> factoryReference) {
        String compIdentifier = compFactoryServiceIdToCompInstIdMapping
            .get(factoryReference.getProperty(Constants.SERVICE_ID).toString());
        componentRegistrationService.unregisterLocalComponentInstallation(compIdentifier);
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
                try (InputStream stream = url.openStream(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    while (true) {
                        int r = stream.read();
                        if (r < 0) {
                            break;
                        }
                        bos.write(r);
                    }
                    return bos.toByteArray();
                } catch (IOException e) {
                    log.warn("Failed to read icon: " + iconPath);
                    return null;
                }
            } else {
                log.warn("Icon not found: " + iconPath);
            }
        }
        return null;

    }

    @Deprecated // TODO (p2) unused; check for removal in 9.0
    protected void registerComponentInstantiationAuthToken(String token) {
        if (token != null) {
            instantiationAuthTokens.add(token);
        }
    }

    private ComponentInterfaceImpl configureComponentInterfaceFromImplementingClass(
        ComponentInterfaceImpl componentInterface, String className) {

        Class<?> componentClass;
        try {
            componentClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            log.error("Failed to load component class: " + className, e);
            return null;
        }

        componentInterface.setLocalExecutionOnly(componentClass.getAnnotation(LocalExecutionOnly.class) != null);
        componentInterface.setPerformLazyDisposal(componentClass.getAnnotation(LazyDisposal.class) != null);
        componentInterface.setIsDeprecated(
            componentClass.getAnnotation(de.rcenvironment.core.component.model.api.Deprecated.class) != null);

        return componentInterface;
    }

    private ComponentInstance createOsgiComponentInstance(ComponentFactory factory, String identifier) {

        Dictionary<String, String> serviceProperties = new Hashtable<String, String>();
        serviceProperties.put(ComponentConstants.COMP_INSTANCE_ID_KEY, identifier);

        try {
            return factory.newInstance(serviceProperties);
        } catch (RuntimeException e) {
            log.error(
                "Failed to load component because of an error in the OSGi DS file or an error in the component's constructor",
                e);
            return null;
        }
    }

    private ServiceReference<?> getComponentReference(String identifier) {

        // search for the previously registered service (component)
        String filter = "(" + ComponentConstants.COMP_INSTANCE_ID_KEY + "=" + identifier + ")";

        ServiceReference<?>[] references;
        try {
            references = osgiComponentCtx.getBundleContext().getAllServiceReferences(Registerable.class.getName(),
                filter);

            if (references == null || references.length == 0) {
                log.error(StringUtils.format(
                    "No component found that provides the service '%s' and that has the temporary identifier '%s'",
                    Registerable.class.getName(), identifier));
                return null;
            } else if (references.length > 1) {
                log.warn(StringUtils
                    .format("More than one component found that provides the service '%s' and that has the temporary identifier '%s',"
                        + " first one is taken", Registerable.class.getName(), identifier));
            }

        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException("Invalid syntax. This is a bug.", e);
        }

        return references[0];
    }

    private ComponentInstallation createComponentInstallation(ComponentRevisionImpl componentRevision) {
        ComponentInstallationImpl componentInstallation = new ComponentInstallationImpl();
        // must be changed in future to be unique per node
        componentInstallation.setInstallationId(componentRevision.getComponentInterface().getIdentifierAndVersion());
        componentInstallation.setComponentRevision(componentRevision);
        componentInstallation.setNodeIdObject(localDefaultLogicalNodeId);
        // For testing purposes:
        // componentInstallation.setNodeIdFromObject(platformService.createTransientLocalLogicalNodeId());
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

        // read and process all the service properties. actually, they are the properties of the component
        setIdentifiers(componentReference, componentInterface);
        componentInterface
            .setDisplayName((String) componentReference.getProperty(ComponentConstants.COMPONENT_NAME_KEY));
        componentInterface
            .setGroupName((String) componentReference.getProperty(ComponentConstants.COMPONENT_NAME_GROUP));
        componentInterface.setVersion((String) componentReference.getProperty(ComponentConstants.VERSION_DEF_KEY));
        componentInterface.setIcon16(readIcon(ComponentConstants.ICON_16_KEY, componentReference));
        componentInterface.setIcon24(readIcon(ComponentConstants.ICON_24_KEY, componentReference));
        componentInterface.setIcon32(readIcon(ComponentConstants.ICON_32_KEY, componentReference));
        componentInterface.setSize(getComponentSize(componentReference));
        componentInterface.setColor(getComponentColor(componentReference));
        componentInterface.setShape(getComponentShape(componentReference));
        componentInterface.setCanHandleNotAValueDataTypes(getCanHandleNotAValueDataTypes(componentReference));
        componentInterface.setIsLoopDriver(getIsResetSink(componentReference));
        componentInterface.setLoopDriverSupportsDiscard(getLoopDriverSupportsDiscard(componentReference));

        try {
            componentInterface
                .setInputDefinitionsProvider(createEndpointDefinitionsProvider(ComponentConstants.INPUTS_DEF_KEY,
                    componentReference, componentReference.getBundle(), EndpointType.INPUT));
        } catch (IOException e) {
            log.error("Failed to parse input definition", e);
            return null;
        }

        try {
            componentInterface
                .setOutputDefinitionsProvider(createEndpointDefinitionsProvider(ComponentConstants.OUTPUTS_DEF_KEY,
                    componentReference, componentReference.getBundle(), EndpointType.OUTPUT));
        } catch (IOException e) {
            log.error("Failed to parse output definition", e);
            return null;
        }

        try {
            componentInterface.setConfigurationDefinition(
                createConfigurationDefinition(ComponentConstants.CONFIGURATION_DEF_KEY, componentReference));
        } catch (IOException e) {
            log.error("Failed to parse configuration definition", e);
            return null;
        }

        try {
            componentInterface.setConfigurationExtensionDefinitions(
                createConfigurationExtensionDefinitions(componentReference.getBundle()));
        } catch (IOException e) {
            log.error("Failed to parse extension definition", e);
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
                log.error(StringUtils.format(
                    "Color declared under '%s' is invalid: %s. Valid ones are: %s. Default will be used: %s",
                    ComponentConstants.COMPONENT_COLOR_KEY, color, Arrays.toString(ComponentColor.values()),
                    ComponentConstants.COMPONENT_COLOR_STANDARD));
            }
        }
        return ComponentConstants.COMPONENT_COLOR_STANDARD;
    }

    private boolean getCanHandleNotAValueDataTypes(ServiceReference<?> componentReference) {
        return Boolean.valueOf(
            (String) componentReference.getProperty(ComponentConstants.COMPONENT_CAN_HANDLE_NAV_INPUT_DATA_TYPES));
    }

    private boolean getLoopDriverSupportsDiscard(ServiceReference<?> componentReference) {
        return Boolean.valueOf(
            (String) componentReference.getProperty(ComponentConstants.LOOP_DRIVER_SUPPORTS_DISCARD));
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
                log.error(StringUtils.format(
                    "Size declared under '%s' is not valid: %s. Valid ones are: %s. Default will be used: %s",
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
                log.error(StringUtils.format(
                    "Shape declared under '%s' is not valid: %s. Valid ones are: %s. Default will be used: %s",
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

        List<InputStream> extendedStaticInputStreams = getInputStreamsForEndpointMetaDataExtensions(reference,
            componentsBundle, type);
        List<InputStream> extendedDynamicInputStreams = getInputStreamsForEndpointMetaDataExtensions(reference,
            componentsBundle, type);

        EndpointDefinitionsProviderImpl endpointProvider = new EndpointDefinitionsProviderImpl();

        Set<EndpointDefinitionImpl> endpointDefinitions;
        try (InputStream staticEndpointDescriptionInputStream = fileUrl.openStream();
            InputStream dynamicEndpointDescriptionInputStream = fileUrl.openStream()) {
            endpointDefinitions = ComponentUtils.extractStaticEndpointDefinition(staticEndpointDescriptionInputStream,
                extendedStaticInputStreams, type);
            endpointDefinitions.addAll(ComponentUtils.extractDynamicEndpointDefinition(
                dynamicEndpointDescriptionInputStream, extendedDynamicInputStreams, type));
        } catch (IOException e) {
            throw new IOException("Failed to parse endpoint definition file: " + file, e);
        }

        endpointProvider.setEndpointDefinitions(endpointDefinitions);

        Set<EndpointGroupDefinitionImpl> inputGroupDefinitions;
        try (InputStream endpointGroupDefinitionInputStream = fileUrl.openStream();
            InputStream staticEndpointGroupDefinitionInputStream = fileUrl.openStream();
            InputStream dynamicEndpointGroupDefinitionInputStream = fileUrl.openStream()) {
            inputGroupDefinitions = ComponentUtils
                .extractStaticInputGroupDefinitions(staticEndpointGroupDefinitionInputStream);
            inputGroupDefinitions.addAll(
                ComponentUtils.extractDynamicInputGroupDefinitions(dynamicEndpointGroupDefinitionInputStream));
        } catch (IOException e) {
            throw new IOException("Failed to parse endpoint group definition file: " + file, e);
        }

        endpointProvider.setEndpointGroupDefinitions(inputGroupDefinitions);
        return endpointProvider;
    }

    private List<InputStream> getInputStreamsForEndpointMetaDataExtensions(ServiceReference<?> reference,
        Bundle componentsBundle, EndpointType type) throws IOException {
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
            return ComponentUtils.extractConfigurationDescription(configurationDescriptionInputStream,
                placeholdersDescriptionInputStream, activationFilterDescriptionInputStream);
        } catch (IOException e) {
            throw new IOException("Failed to parse configuration definition", e);
        }
    }

    private Set<ConfigurationExtensionDefinitionImpl> createConfigurationExtensionDefinitions(Bundle componentsBundle)
        throws IOException {

        Set<ConfigurationExtensionDefinitionImpl> descs = new HashSet<ConfigurationExtensionDefinitionImpl>();

        for (Bundle fragment : getFragmentBundlesProvidingPropertyExtensions(componentsBundle)) {
            Dictionary<String, String> manifestHeaders = fragment.getHeaders();
            String extConfigFile = manifestHeaders
                .get(ComponentConstants.MANIFEST_ENTRY_RCE_COMPONENT_EXTENSION_CONFIGURATION);
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
            return ComponentUtils.extractConfigurationExtensionDescription(configurationDescriptionInputStream,
                placeholdersDescriptionInputStream, activationFilterDescriptionInputStream);
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
