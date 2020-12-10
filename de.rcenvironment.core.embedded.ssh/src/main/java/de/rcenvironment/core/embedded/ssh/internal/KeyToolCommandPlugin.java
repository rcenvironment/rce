/*
 * Copyright 2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.embedded.ssh.api.SshAccountConfigurationService;
import de.rcenvironment.toolkit.utils.common.IdGenerator;
import de.rcenvironment.toolkit.utils.common.IdGeneratorType;

/**
 * Execution handler for "keytool" commands. Currently located in the SSH bundle for simplicity; could also be moved elsewhere or split up.
 *
 * @author Robert Mischke
 */
@Component
public class KeyToolCommandPlugin implements CommandPlugin {

    private static final String SUBCMD_SSH_PW = "ssh-pw";

    private static final String SUBCMD_UPLINK_PW = "uplink-pw";

    private static final String MAIN_CMD = "keytool";

    // 14 characters of Base64 minus two characters (~6 bits/char) gives ~80 bits of entropy, which is much more than
    // a typical human-generated password, while still being reasonable to type if necessary. -- misc_ro
    private static final int GENERATED_PASSWORD_LENGTH = 14;

    @Reference
    private SshAccountConfigurationService sshAccountService;

    @Override
    public void execute(CommandContext context) throws CommandException {
        context.consumeExpectedToken(MAIN_CMD);
        String subCmd = context.consumeNextToken();
        List<String> parameters = context.consumeRemainingTokens();
        if (SUBCMD_SSH_PW.equals(subCmd) || SUBCMD_UPLINK_PW.equals(subCmd)) {
            performGenerateSshOrUplinkPw(context, parameters);
        } else {
            throw CommandException.syntaxError("Missing operation argument (e.g. \"" + MAIN_CMD + " " + SUBCMD_SSH_PW + "\")",
                context);
        }
    }

    private void performGenerateSshOrUplinkPw(CommandContext context, List<String> parameters) {
        // To ensure an unbiased password without the special Base64url characters ("_" and "-"), over-generate at twice the length...
        String password = IdGenerator.createRandomBase64UrlString(GENERATED_PASSWORD_LENGTH * 2, IdGeneratorType.SECURE);
        // ...and then remove the offending characters and trim to the actual length. For this to fail, more than half of the
        // generated characters would have to be the 2 out of 64 ones removed. This is extremely unlikely, and even in that
        // case, the result would not be broken, but only a slightly shorter password. -- misc_ro
        password = password.replaceAll("[-_]", "").substring(0, GENERATED_PASSWORD_LENGTH);

        context.println("The generated password (keep this confidential):");
        context.println(password);
        context.println("The password hash (send this to the server's administrator):");
        context.println(sshAccountService.generatePasswordHash(password));
    }

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<CommandDescription>();
        // TODO staticPart/dynamicPart is not always used as intended here to fix problems with the help command,all ComamndDescriptions
        // should be revisited when new command help/parser is in place
        contributions.add(new CommandDescription(MAIN_CMD, SUBCMD_SSH_PW,
            false, "generates a password for an SSH connection, and the corresponding server entry"));
        contributions.add(new CommandDescription(MAIN_CMD + " " + SUBCMD_UPLINK_PW, null,
            false, "generates a password for an Uplink connection, and the corresponding server entry"));
        return contributions;
    }

}
