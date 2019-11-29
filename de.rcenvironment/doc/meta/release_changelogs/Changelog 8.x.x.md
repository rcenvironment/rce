##### RCE 8.3.0 (released on July 05, 2019)

###### TiGL Viewer

* Decoupled the distribution of TiGL Viewer from RCE (i.e., it is not included in RCE releases anymore)
* Made the path of the TiGL Viewer configurable

###### Libraries

* Migrated all remaining uses of Jackson 1.x to Jackson 2.x (9.x.x backport)
* Upgraded Jackson 2.9.8 to Jackson 2.9.9

###### Licensing

* Updated license information

###### Bugfixes

* Fixed minor NPEs

	
##### RCE 8.2.3 - Red Snapper (released on Nov 15, 2018)

###### Workflow Engine

* Significantly reduced the RAM usage of workflow and component hosts
* Added rate limiting for remote component initialization to prevent network timeouts in complex workflows
* Fixed an issue that prevented workflows from being cancelled in the "preparing" state

###### Security

* Addressed several security issues by upgrading libraries and securing the way archives are unpacked internally ("Zip Slip" vulnerability)

###### Misc/Internal

* Improved, compacted and reduced log output
* Added unit tests for internal archive handling
* Added automatic debug-level logging of large network messages

##### RCE 8.2.2 - Red Snapper (released on May 30, 2018)

* Upgraded external librariers (Apache MINA, SSHD) to fix an SSH connection issue that occurs due to changes in Java version 8u161


##### RCE 8.2.1 - Red Snapper (released on May 24, 2018)

* Fixed a bug in the Optimizer component. Lower and upper bounds were not applied properly when using the input data type Vector
* Minor fix in the TiGL Viewer process handling
* Minor logging and other internal improvements


##### RCE 8.2.0 - Red Snapper (released on February 02, 2018)

###### Synopsis

* Added a new workflow component 'XML Values' to read and write values from XML files
* Improved UI support for exporting simulation data into projects
* Fixed a performance issue in complex workflows with nested loops


###### SSH and Remote Access

* Restored the default SSH inactivity timeout of 10 minutes which was accidentally set to 10 seconds in 8.1.0

###### Tool Integration

* Added the missing 'Queue' input handling to dynamic inputs of integrated CPACS tools

###### Components

* New workflow component: XML Values 
* Enhanced the XPath chooser dialog
* OutputWriter: Added UI support to set the root location relative to the current workspace
* OutputWriter: When writing to a workspace relative root location a placeholder makes the location explicit; relative paths without this placeholder will be rejected in RCE 9 and later.

###### GUI

* Added 'Import...' and 'Export...' menu entries to the file menu
* Added the Command Console to the views that are openen by default with a fresh profile
* Integrated PDF viewer (PDF4Eclipse): Set a different default renderer to enhance compatibility
* Improved the sorting options of the Workflow Data Browser
* Improved the Timeline view to avoid zooming and refreshing issues
* Added Workflow Data Browser support for exporting to the currently selected folder in the Project Explorer
* Fixed and improved the automatic separation of host and port when filling in the network connection dialog using copy-and-paste

###### Documentation and Help

* Updated and improved the developer guide regarding Eclipse formatter settings and the Workspace Mechanic plugin

###### Misc/Internal

* Fixed a performance issue with nested loops that caused workflows with large numbers of inputs/outputs to slow down drastically, sometimes resulting in time outs
* Script components are now also properly reset within nested loops between loop iterations
* Improved stability when shutting down RCE instances quickly after their startup
* Added an internal BDD test scripting feature (still under development) with basic documentation
* Added "--use-default-workspace" launch option for skipping the GUI workspace chooser dialog
* Various improvements to the "Instance Management" feature (still in development), e.g. proper handling of aborted downloads
* Improved TiGLViewer process handling to prevent UI freezes on first start
* Fixed a bug that prevented the execution of workflows with nested loops if there was a high number of connections between the components within the nested loop.
* Fixed some rare unit test failures
* Various code quality, log output, and build process improvements


##### RCE 8.1.1 - Red Snapper (released on November 17, 2017)

* Internal improvements to enhance building RCE as an external developer
* Improved and expanded the user and developer guides
* Fixed bugs in fault tolerant loop's button activation and runtime behavior
* Fixed a bug that caused tools and workflows executed via SSH connections to create an additional (incorrect) directory level when writing to an output
* Fixed a bug in CPACS Tool Integration that caused tool imitation mode not to work properly
* Several minor fixes

##### RCE 8.1.0 - Red Snapper (released July 14, 2017)

###### Synopsis

* Bidirectional connections in a workflow are now separated to improve visual clarity
* Added a dialog for profile selection at start up ("rce -p"), which also supports changing the default profile
* Dropped support for 32-bit operating systems


###### Workflow Editor

* Enhanced zooming capabilities
* Changed depiction of bidirectional connections to two separate lines
* Added functionality to export the workflow as an image file
* A small icon in a component indicates if it runs "Local only" or in "Tool imitation run mode". That icon is now also shown in Runtime Workflow Editor.

###### DOE Component

* Added capability to receive the table for custom table design method as input at runtime

###### Driver Components

* Start values of forwarding inputs can now be declared as constant

###### Tool Integration

* CPACS Tool Integration: Fixed logical path of output file existence check
* Enhanced templates for CPACS tools

###### SSH / Remote Access

* Fixed temporary SCP folders not being deleted after Remote Access sessions
* Added option to customize the SSH session timeout (which must be set for RA tools that run longer than the default of 10 minutes)

###### Startup

* Added a dialog to select a profile to start with (command line: "rce -p")
* Improved error handling during startup
* Fixed a startup crash issue when starting several RCE instances at once
* Reduced disk activity on startup

###### Misc/Internal

* Fixed a bug which assigned the wrong owner to a log file during installation of the RCE daemon
* Fixed a bug which erased the selected target execution instance during a workflow update
* Fixed an issue in the network view (Published components' subtree was not shown in some cases)
* Fixed handling of expanded states of nodes in the Network View and Workflow Data Browser
* Temporary files for remote tool access are now cleaned up after successful execution
* Added an Eclipse-style popup for successful exports from Workflow Data Browser
* Major changes to the source repository structure and the handling of external binaries (Dakota/TiGLViewer)
* Completely removed support for 32-bit builds (which were only disabled before) for maintainability
* Eliminated the need to copy the Dakota binaries to a temporary location on every start
* Migrated the build process to JDK 8 and Maven >= 3.2.0 (which are now also required for building RCE)

##### RCE 8.0.2 - Red Snapper (released on March 07, 2017)

* Fixed a bug in the CPACS Tool Integration which resulted in a failed output mapping, if the CPACS tool was configured to "Use a new working directory on each run" and to "Copy tool to working directory on each run" and the "Only run on changed inputs" feature was enabled
* Fixed a bug in the CPACS Tool Integration which prevented the correct forwarding of additionally cached values, if the "Only run on changed inputs" feature was enabled
* Minor adjustments to debug and log messages

##### RCE 8.0.1 - Red Snapper (released on December 16, 2016)

* Fixed a bug in Script component's help which showed another component's help
* Fixed a bug when deleting forwarding inputs of driver components (Converger, Design of Experiments, Optimizer, Parametric Study)
* Fixed a bug in the Design of Experiments component where design values were not stored correctly in the data management. As a result the nodes "table.csv" and "result.csv" in the Workflow Data Browser showed wrong content. Nevertheless the correct design values were sent to the workflow.

##### RCE 8.0.0 - Red Snapper (released on December 06, 2016)

###### Synopsis

* Semi-automated workflow execution: Added capability to make workflow execution semi-automated, i.e. a tool integrator can approve or deny tool results before they are sent further to succeeding workflow components
* Workflow Editor: Added capability to move components and labels before or behind each other
* Removed 32-bit version of RCE from the build process so they are not available anymore
* Many minor fixes and improvements


###### Workflow Editor

* Added loop level information of inputs/outputs to their respective tables of workflow driver components
* Added a read-only version of the Workflow Editor to inspect workflows from the Workflow Data Browser
* Added capability to move components before or behind each other. The same holds for labels. Components are always placed "before" labels 
* Tool Run Imitation Mode:
  * Tools that are configured to run in the Tool Run Imitation Mode, are now marked with a small "I" and have a blue background color
  * Tool Run Imitation Mode can be enabled and disabled via the component's context menu
* Added F1 help for connections, the workflow editor and workflow labels
* Enhanced properties of workflow labels, e.g. adding an optional header for every label
* Fixed caret positioning in workflow labels
* Fixed a bug with undo/redo in combination with component endpoints

###### Components

* Design of Experiments: Added output "Number of samples"
* Evaluation Memory: Added capability to be used in nested loops
* Optimizer: Added capability to load default values in the "Algorithm Properties" dialog
* OutputWriter: Improved error handling when writing files with names which are forbidden by the operating system
* Parametric Study and Design of Experiments: In case the workflow is cancelled they now stop sending design values. It caused problems in case they are used without any loop
* Parametric Study: Fixed a bug that caused the ordering of the results in the Workflow Run editor to become inconsistent
* Parametric Study: Fixed issues with descending sampling
* Script: Added a validation consistent indentation usage. Only either whitespaces or tabs are allowed as mixing them caused problems
* SQL components: Removed deprecated components SQL Reader, SQL Writer and SQL Command. For accessing databases the Database component can be used.
* TiGL-Viewer: Fixed an issue that caused RCE to freeze occasionally
* XML Merger: User specified messages using the XSLT command <xsl:message> are now displayed in the Workflow Console
  
###### Tool Integration

* Added support to select the shell that is used to execute commands in the Linux execution window
* Added capability to use properties as placeholders in the scripts. They are defined on the properties page
* Fixed a bug with tool and working directory relative root paths
* Fixed a bug when using iteration directories
* Fixed a bug when adding a new launch setting to tool configuration
* Added a constraint that working directories must be absolute because relative ones caused problems in some setups

###### Workflow Data Browser

* Data types of inputs now show the converted data type instead of the original one in case of conversion. Additionally a hint about the conversion is shown.
* Added a marker to the icon of a component in case the component run failed
* Fixed "Refresh selected" hotkey
* Fixed unintuitive behavior when expanding leaf nodes via double click

###### Workflow Execution

* Removed the "Loop done" approach for nested loop configuration, i.e. the user does not have to add a connection between loop drivers anymore to estabilish a nested loop setup
* Added capability to make workflow execution semi-automated, i.e. a tool integrator can approve or deny tool results before they are sent further to succeeding workflow components
* Fixed a bug that caused the timestamp in a workflow run name to get broken, so all subsequent workflow runs had the same name in the workflow data browser
* Changed the behavior when saving passwords: When you uncheck the "save" option and execute the workflow the saved password will be discarded

###### GUI Misc

* When starting RCE in headless mode the splash screen is now automatically disabled
* Added an option to copy the full path of resources in the Project Explorer via its context menu
* Added line numbers to script text fields, i.e. in the Script component, Cluster component and the Tool Integration wizard
* Enhanced layouts of the property views of Script component, workflow labels and components of integrated tools

###### Validation

* Added a validation for the configured python executable location
* Added a validation report that is shown when a workflow to be executed contains validation errors
* Moved validation code to backend

###### SSH/Remote Access

* Replaced regular expression-based SSH roles with predefines ones
* Added an option to include system information (average CPU load, free RAM) in tool list queries; that is useful for monitoring and load balancing

###### Network

* Fixed an issue where clients could not connect to a server if their system clocks were out of sync
* Instances in a network now recognize when another instance in the network was restarted, which ensures consistent behavior
* Made the network more robust in case of duplicate instance identifiers (which can occur when profile folders are mistakenly copied and reused)
* Added support for splitting each instance into multiple "logical" instances. As a first use case, this is used to ensure consistent behavior when forwarding components via SSH Remote Access.
* Reduced management overhead when forwarding workflow console output in larger networks

###### Misc/Internal

* Updates are now fetched via HTTPS
* Minor performance improvements
* Removed 32-bit versions of RCE from build process
* Added the capability to send e-mails (useful in conjunction with semi-automated workflow execution)
* Added the capability to start RCE from an arbitrary working directory
* Fixed a minor shutdown issue that had no impact but caused warnings in the log
* Scripts can handle Infinity/NaN values
* Fixed a bug when using outputs of type "not a value"
* Various minor fixes and improvements

