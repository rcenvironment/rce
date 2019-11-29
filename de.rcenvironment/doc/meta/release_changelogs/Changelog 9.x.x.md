##### RCE 9.1.1 (June 26, 2019)

###### Bugfixes

* Fixed incorrect display of error messages in the tool integration wizard
* Fixed a display error in the workflow data browser that occurred with very short tool executions
* Fixed an error that allowed to edit preconfigured outputs of the Switch component
* Fixed errorneous behavior when opening component help via the context menu
* Minor other fixes


##### RCE 9.1.0 (June 07, 2019)

###### TiGL Viewer

* Decoupled the distribution of TiGL Viewer from RCE (i.e., it is not included in RCE releases anymore)
* Made the path of the TiGL Viewer configurable

###### Licensing

* Updated license information

###### Bugfixes

* Fixed erroneous "undo" behaviour for database component, integrated tools, switch component, evaluation memory, output writer, converger and XML loader component
* Fixed a bug in accessing tool documentation via SSH connections


##### RCE 9.0.0 (March 13, 2019)

###### Workflow Execution

* Workflow execution is now robust against temporary network disconnects and overloaded instances
* It is now explicitly verified that the workflow controller can access all components before starting a workflow
* Added "wf show" command to open the runtime view of an executing workflow
* Improved "wf details" command output
* Improved general error handling

###### Authorization

* Added group-based authorization for components: Users can create any number of groups, invite each other, and publish their components for specific groups or make them public
* Added a dialog for managing authorization groups
* Added a 'Component Publishing' view. Here one can publish components either as public or assign them to specific authorization groups
* Added console commands for managing authorization groups and component permissions

###### Tool Integration

* Introduced stricter and more consistent naming rules for component ids/names, versions, and assigned group names. Note that local components that do not match these new rules must be renamed before they can be published again.
* Added "export" option to download and store tool documentation
* Added validation to already existing tool names

###### Remote Access

* All published tools can now also be accessed over SSH connections
* Tool documentation is now also accessible over SSH connections
* Added "auto-reconnect" option for SSH connections
* Enabled cancelling of remote tools
* For workflows published for remote access, the Palette group can now be configured
* Add a flag to the ra-publish command to prevent "Dispose on Success"

###### Components

* Added tolerance settings to evaluation memory
* Enhanced placeholder validation and replacement in the output writers data sheet
* New helper components 'SCP Input Loader' and 'SCP Output Collector' for remote access workflows.
* The TiGL Viewer Component does now also accepts file types other than CPACS.

###### Misc./Internal

* Upgraded the underlying Eclipse platform to Eclipse Photon
* Switched to Java 8u161 as minimum runtime requirement
* Upgraded to Jackson 2
* Made RCE runnable on newer Windows platforms
* Rearranged menus and menu items
* Improved documentation
* Improved Checkstyle ruleset
* Upgraded .deb repository signatures to SHA-256
* Made the internal secure storage more robust, and also usable in headless mode
* Disabled GPG key import and Linux repository setup scripts until they can be properly updated
* Added a solution for parallel handling of binary artifacts with SVN and Git-LFS, removing the need for post-release Git additions
* Ensured proper build and Eclipse workspace import when checking out from the Git mirror repository
* Made the "Test Script Runner" used for self-verification a standard part of all builds
* Added SPDX-compliant license headers to all source files
* Various GUI enhancements

###### Bugfixes

* Fixed a bug that forbid the result verification feature to work with CPACS tools
* Fixed a bug that does not delete the working directory if the XML mapping of integrated CPACS tools fails
* Fixed a bug which lead to leftover workflow files in the RCE temp directory
* Fixed a bug which caused severe performance issues in the workflow execution wizard on GTK systems
* Fixed erroneous "undo" behaviour for optimizer, script and design of experiments component and partially for database component
* Various minor fixes

###### Profile Data Changes

* Previous component publication entries in configuration.json and published.conf have no effect anymore. Use the new authorization system to re-publish these components as intended. You may need to delete the old publication entries manually to eliminate the (harmless) warnings on RCE startup.
* Due to internal secure storage changes, any stored passwords, placeholder values, and Cluster Job Monitoring entries must be entered again.
* There will be a dialog prompting for a profile upgrade upon first startup of RCE 9.0
