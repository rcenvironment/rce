/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.examples.decrypter.execution;

import java.io.IOException;
import java.text.MessageFormat;

import de.rcenvironment.components.examples.decrypter.common.DecrypterComponentConstants;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.utils.encryption.EncryptionFactory;
import de.rcenvironment.core.utils.encryption.PassphraseBasedEncryption;

/**
 * 
 * Main class for the component logic. In this class the component lifecycle is done. Most of the methods have a default implementation
 * which is done in the {@link DefaultComponent} class. If the component should have an individual behavior, the appropriate methods must be
 * overridden.
 * 
 * 
 * @author Sascha Zur
 */
public class DecrypterComponent extends DefaultComponent {

    private ComponentContext componentContext;

    private ComponentDataManagementService dataManagementService;

    private TypedDatumFactory typedDatumFactory;

    private boolean useDefaultPassphrase;

    private String decryptionAlgorithm;

    private PassphraseBasedEncryption encryption;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public void start() throws ComponentException {
        dataManagementService = componentContext.getService(ComponentDataManagementService.class);
        typedDatumFactory = componentContext.getService(TypedDatumService.class).getFactory();

        // {@link ComponentContext} is interface to the workflow engine

        // Reading the configuration of a component is done via the {@link ComponentContext}
        useDefaultPassphrase =
            Boolean.parseBoolean(componentContext.getConfigurationValue(
                DecrypterComponentConstants.CONFIG_KEY_USEDEFAULTPASSWORD));

        decryptionAlgorithm = componentContext.getConfigurationValue(DecrypterComponentConstants.CONFIG_KEY_ALGORITHM);

        // Create factory for encryption algorithms and create encryption instance
        EncryptionFactory factory = new EncryptionFactory();

        // Find the correct algorithm
        encryption =
            factory.createPassphraseBasedEncryption(EncryptionFactory.PassphraseBasedEncryptionAlgorithm.valueOf(decryptionAlgorithm));
    }

    @Override
    public void processInputs() throws ComponentException {

        FileReferenceTD incEncryptedFileReference =
            (FileReferenceTD) componentContext.readInput(DecrypterComponentConstants.INCOMING_ENPOINT_ENCRYPTED_FILE);

        // The incoming value is just a reference to a file in the data management.
        // So, get the content of the file
        String encryptedString;
        try {
            encryptedString = dataManagementService.retrieveStringFromReference(incEncryptedFileReference.getFileReference(),
                componentContext.getDefaultStorageNodeId());
        } catch (IOException e) {
            encryptedString = null;
        } catch (CommunicationException e) {
            throw new RuntimeException(MessageFormat.format("Failed to retrieve string from data reference from remote node @{0}: ",
                componentContext.getNodeId())
                + e.getMessage(), e);
        }
        // Now decrypt the String
        if (encryptedString != null) {
            String passphrase;
            if (useDefaultPassphrase) {
                passphrase = DecrypterComponentConstants.DEFAULT_PASSWORD;
            } else {
                passphrase =
                    componentContext.getConfigurationValue(
                        DecrypterComponentConstants.CONIG_KEY_DECRYPTING_PASSPHRASE);
            }
            String decryptedString = encryption.decrypt(encryptedString, passphrase);

            if (decryptedString != null) {
                // To send the decrpypted String to the next workflow component, it must be
                // sent as a {@link TypedDatum} object, in this case a {@link ShortTextTD} is
                // appropriate.
                // For creating {@link TypedDatum} object, the {@link TypedDatumFactoy} must always
                // be used.
                ShortTextTD outputText = typedDatumFactory.createShortText(decryptedString);

                // A {@link TypedDatum} object is sent to the next workflow component by writing it
                // to an output.
                // An output is identified by its unique name
                // Note: Closing an output means that no more values will be sent.
                componentContext.writeOutput(DecrypterComponentConstants.OUTPUT_NAME, outputText);

                // After sending the result to the output, it also should appear on the workflow
                // console
                componentContext.getLog().componentInfo(decryptedString);
            } else {
                throw new ComponentException("Could not decrypt file! Wrong password or decrypting algorithm?");
            }
        }
    }

}
