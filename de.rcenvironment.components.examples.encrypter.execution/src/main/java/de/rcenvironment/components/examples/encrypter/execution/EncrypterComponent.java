/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.examples.encrypter.execution;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.examples.encrypter.common.EncrypterComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.encryption.EncryptionFactory;
import de.rcenvironment.core.utils.encryption.PassphraseBasedEncryption;

/**
 * Class for encryption component logic.
 * 
 * @author Sascha Zur
 */
public class EncrypterComponent extends DefaultComponent {
    
    private static final Log LOG = LogFactory.getLog(EncrypterComponent.class);

    private ComponentContext componentContext;
    
    private ComponentDataManagementService dataManagementService;

    private String encryptionAlgorithm;

    private File workingDirectory;

    private PassphraseBasedEncryption encryption;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }
    
    @Override
    public void start() throws ComponentException {
        dataManagementService = componentContext.getService(ComponentDataManagementService.class);

        encryptionAlgorithm = componentContext.getConfigurationValue(EncrypterComponentConstants.CONFIG_KEY_ALGORITHM);

        EncryptionFactory factory = new EncryptionFactory();

        if (encryptionAlgorithm != null) {
        // Find the correct algorithm
            encryption =
                factory.createPassphraseBasedEncryption(EncryptionFactory.PassphraseBasedEncryptionAlgorithm.valueOf(encryptionAlgorithm));
        } else {
            throw new ComponentException("No encryption algorithm is selected.");
        }
        // When working on the file system, a working directory is required. For this, RCE provides
        // the opportunity to create such a directory in a managed RCE temp directory.
        // Note that those files must be deleted after the component is done (or failed, ...)
        try {
            workingDirectory = TempFileServiceAccess.getInstance().createManagedTempDir();
        } catch (IOException e) {
            LOG.error("Could not create working directory: ", e);
            // if something goes wrong that prevents the component from working correctly,
            // a {@link ComponentException} must be thrown
            throw new ComponentException("Could not create working directory: " + e.getMessage());
        }
    }
    
    @Override
    public void processInputs() throws ComponentException {
        String textToEncrypt = "";
        for (String key : componentContext.getInputsWithDatum()) {
            TypedDatum input = componentContext.readInput(key);
            // Check the data type of an input
            if (input.getDataType() != DataType.FileReference) {
                textToEncrypt += input.toString() + File.separator;
            } else {
                // Read the meta data of an input/output
                if (componentContext.getInputMetaDataValue(key, EncrypterComponentConstants.METADATUM_USAGE_OF_FILE)
                    .equals(EncrypterComponentConstants.METADATUM_VALUE_USAGE_NAME)) {
                    textToEncrypt += ((FileReferenceTD) input).getFileName();
                } else {
                    try {
                        // Create a new file from a @link{FileReferenceTD} and write it in a
                        // unique tmp
                        // file
                        File incFile = new File(workingDirectory, "temp-file-" + UUID.randomUUID().toString());
                        dataManagementService.copyReferenceToLocalFile(((FileReferenceTD) input).getFileReference(),
                            incFile, componentContext.getStorageNetworkDestination());
                        // read a file to a string using FileUtils
                        textToEncrypt += FileUtils.readFileToString(incFile) + "\n";
                    } catch (IOException e) {
                        LOG.error(e);
                    }
                }
            }
        }

        // Create temp file for encrypted text
        File encryptedResult = new File(workingDirectory, encryptionAlgorithm + ".data");

        String passphrase = componentContext.getConfigurationValue(
            EncrypterComponentConstants.CONFIG_KEY_ENCRYPTION_PASSPHRASE);

        // Encrypt
        String encryptedText = encryption.encrypt(textToEncrypt, passphrase);

        if (encryptedText == null) {
            throw new ComponentException("Encryption failed!");
        }

        try {
            // Write encrypted text to file. Use {link@ FileUtils}.
            FileUtils.writeStringToFile(encryptedResult, encryptedText);

            // Before sending a file to the next workflow component, it must be written into the
            // data management. This is
            // done by the {@link ComponentDataManagementService}. Reference to the file is returned
            // which will be sent
            FileReferenceTD encryptedFileReference =
                dataManagementService.createFileReferenceTDFromLocalFile(componentContext, encryptedResult, encryptedResult.getName());
            
            componentContext.writeOutput(EncrypterComponentConstants.OUTPUT_NAME_RESULT, encryptedFileReference);

        } catch (IOException e) {
            LOG.error("Could not write encrypted file: ", e);
            throw new ComponentException("Output could not be written!");
        }
        componentContext.getLog().componentInfo(encryptedText);
    }

    @Override
    public void tearDown(FinalComponentState state) {
        try {
            if (workingDirectory != null) {
                // Clean up temp files created.
                TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(workingDirectory);
            }
        } catch (IOException e) {
            LOG.error(e);
        }
    }

}
