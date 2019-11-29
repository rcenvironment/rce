##### RCE 6.3.0 - Kapalua Butterfly (released at 29 July 2015)

###### Synopsis

* This release fixes a security issue; users that run components on untrusted machines should upgrade to 6.3.0 as soon as possible
* Various minor bugfixes and improvements
* Workflow Data Browser: Added icons to the top-level workflow entries that show the final state of workflows 


###### Components

* XML Merger: Added input handling "queue" to static input "XML to integrate"
* Design of Experiments: Improved cleanup of temp directories
* Design of Experiments: The component can now be used in outer loops
* Design of Experiments: Improved validation
* Input Provider: Fixed selection of files and folders in imported projects or projects located outside the workspace
* CPACS Writer: Improved cleanup of temp directories

###### Workflow Data Browser

* Added icons to the top-level workflow entries that show the final state of finished workflows; running workflows keep the default workflow icon
* Fixed saving of execution log files in case of an error
* Fixed working directory to be shown for CPACS components

###### Tool Integration

* Added a file containing the running workflow's name to the working directory of tools ("rce-workflow-information.txt")
* Fixed "Run only on changed inputs" option
* Fixed path for directory inputs when using placeholder variables


###### Security

* This release fixes a security issue; users that run components on untrusted machines should upgrade to 6.3.0 as soon as possible

###### Console Commands

* The output of "wf run" now includes the workflow id needed for other commands
* Added a <code>"--compact-output"</code> flag to "wf run" to simplify scripted usage

###### Remote Access

* Made a minor protocol change that allows consistent behavior for Debian 7 clients

###### Internal

* Increased the heap size setting for RCE instances running on 32-bit Java by 20%
* Various minor bugfixes and improvements
* Reduced debug log output volume to make it easier to inspect

##### RCE 6.2.1 - Kapalua Butterfly (released at 24 June 2015)

* Fixed broken directory structure for files and directories, which are received as input values by the script component and by integrated tools
* Removed reliably XSLT mapping files from working directories of integrated CPACS tools (in some cases they are left)
* Endpoints can be deleted again even if they are currently connected
* Due to some issues, the renaming option for files and directories received by integrated tools is removed (it is always "-As incoming-" now) 


##### RCE 6.2.0 - Kapalua Butterfly (released at 12 June 2015)

###### Synopsis

* Improved usability of various graphical user elements including log messages
* Extended and improved the access of tools and workflows via SSH (remote access)
* Fixed minor bugs
* Internal improvements (e.g., improved quality assurance by increasing unit test coverage) 


###### Usability

* Script component
	- Files and directories (received as input values) keep their name (affects e.g. the name shown in the workflow data browser)
	- Added support to show whitespace characters in the text field used to write the Python script 
* Tool integration dialog
	- Added support for editing values (inputs, outputs, properties etc.) on double click
	- Added dialog which helps to create Python commands to copy files or directories within the pre or post execution script 
* Workflow data browser
	- The host instance of each workflow component is now shown
	- Added possibility to open CPACS files in the TiGL viewer from the workflow data browser's context menu 
* Workflow list
	- Workflows can be deleted now with the Del key
	- Fixed default sorting (now by time) 
* Command console: added feature to clear command console line on Esc
* Workflow console: added possibility to copy either a whole console line or just the message itself
* Announce syntax errors in the main configuration file of RCE at start up
* Workflow execution
	- Improved warning shown in the workflow execution dialog if workflow components are not available or only available with other versions (which prevents the user from executing a workflow at all)
	- Improved warning when component versions of workflow file and current installation don't match
	- Added the new placeholders "${profileName}" and "${version}" to instance name configuration; this is intended to simplify deployments with many instances 

###### Remote Access (access of tools and workflows via SSH) 

* Workflows published with the "ra-admin publish-wf" command are now persistent by default (ie, they are still available after restarting the RCE instance)
* Tools on the RCE instance running the SSH server must now be explicitly published to be available
* Clients can now explicitly select the instance of a tool that is available on multiple RCE instances
* Filtered the "list-tools" output to tools that actually match the Remote Access requirements
* The client's tool selection is now checked against the published/available tools before execution; before this, invalid values simply caused the generated workflow to fail
* Improved error handling and feedback messages 

###### Bug Fixes 

* Joiner component: fixed the configuration GUI of the joiner component so that its elements are applied correctly after undo or redo
* Command console: Fixed pasting from clipboard into the command console on linux systems (fixed delay and issue with line break)
* Fixed writing integer values to outputs of type float in script component and post script of integrated tools
* Workflow data browser: fixed exporting directories from the workflow data browser
* Files and projects can be deleted from workspace projects now even if they were opened beforehand
* Workflow editor: workflow components, which are not available, can be now copied/cutted and pasted as they were be available
* Fixed resource leaks in workflow data browser and workflow timeline view
* Workflow engine: constant inputs of loop driver components (e.g. Optimizer, DOE, ... ) are now reset if loop was reset (mainly affects loop drivers of nested loops) 

###### Internal Improvements 

* Improved quality assurance by increasing unit test coverage
* Refactor the code base responsible for loading, mapping, and saving XML files


##### RCE 6.1.0 - Kapalua Butterfly (released at 16 April 2015)

###### Workflow Components 

* Added new component: Switch 
	- Directs data on the base of a condition to one of two outputs 
* Added new component: Design of Experiments (DOE) 
	- Supports sampling of a bounded solution space 
* Converger: Replaced "maximum iterations" by "maximum convergence checks" 
* Parametric Study: Added option "fit to bounds"
* Optimizer 
	- Added support for mixed integer optimization 
	- Integrated new algorithm method called NOMAD, which is more applicable for mixed integer optimization 

###### Tool Integration 

* Extended options for the deletion of working directories: they can be kept in the case of an error now
* Pre and post processing scripts now have the "shutil" module imported by default to simplify file and directory copying
* Enabled tabbing through the "Tool Description" page of the tool integration wizard 

###### Workflow Editor

* Added possibility to define custom connection paths
* Added support for workflow labels 

###### Workflow Data Browser 

* Improved performance when fetching the list of workflows and when expanding timelines
* Fixed an error in the graphical tree view which sometimes occured when deleting workflows ("Comparison method violates contract")
* Fixed an error which allowed running workflows to be deleted under certain circumstances 

###### Workflow Data Management 

* Improved stability and performance (especially for workflows with a high number of component runs - in the order of >10,000 runs) 

###### GUI Miscellaneous 

* Improved the performance of the workflow list view (workflows and their states were updated very slowly from remote workflow hosts)
* Changed the font (Courier) for the command console and script editing areas (the Script component's editor and the tool integration wizard's pre and post processing pages)
* Fixed layout of the input table of the OutputWriter component
* Validation of workflow components' configuration is now only done when saving the workflow; this improves GUI performance
* Validation errors and/or warnings are now checked at workflow start again and are shown to the user with a popup dialog
* Workflow execution wizards shows the actual instance names as they appear in the network view
* The "Apply to all" button in the placeholder page of the workflow execution wizard copies the placeholder only to other placeholders of the same data type and the same component type now
* Improved the insert locations of pasted workflow nodes; prevented the case where inserted components have the same position, which made them hard to tell apart
* Connection Editor: Inputs which are required but not connected yet are marked with a red asterisk 

###### Remote Access  

* Added a command to only list components that are compatible with the "run-tool" requirements
* Made command output easier to parse by client code (used by upcoming C/C++ API)
* Added a default version ("1") to published workflows so "run-wf" and "run-tool" can use the same API calls 

###### Console Commands 

* Added console commands to pause, resume, cancel, and dispose workflows
* Added console command to list all active workflows (wf list) 

###### Configuration

* Changed the embedded SSH server's "host" parameter to "ip" for clarity ("host" still works, but a warning is logged)
* Fixed some configuration documentation issues 

###### Deployment

* Provided 32-bit .deb packages (in addition to existing 64-bit packages) 

###### Documentation

* Added documentation for concept behind the CPACS mapping of integrated CPACS-capable tools 

###### CPACS

* Updated namespace for map in cpacs mapping files (old one still works, but a warning is logged) 

###### Miscellaneous

* Disabled the "<profile>/output/console.combined.*.log" files to conserve disk space; if you were using them, please contact us for discussion of this feature
* Removed irrelevant stack traces to reduce log volume
* Improved log output of network error situations
* Added "--profile" option as an alternative/alias to "-p"
* Fixed the profile directory's name not being used for the temp directory prefix (was always "default")
* Cleaned up the command console's "help" output
* Script component (Jython) now sends unique NaV values to the affected outputs within one run
* A workflow component, which triggers that a workflow is finished, doesn't forward the finish information anymore (this led to warnings in the log)
* Reduced callback attempts of a workflow component if its workflow controller is not reachable anymore; reduced number of error messages in the log
* Increased the automated test coverage (especially, workflow engine, data management, workflow components)
* Generation of "Output/autoExportOnStartup.json" is now optional (default: off) 


##### RCE 6.0.3 - Kapalua Butterfly (released at 03 February 2015)

* Reduced heap size (declared in rce.ini) for Windows 32bit version of RCE
* Added configuration parameter for RCE profiles to the windows service installation script 


##### RCE 6.0.2 - Kapalua Butterfly (released at 28 January 2015)

* Fixed tool integration wizard, which failed to integrate a tool under some certain circumstances
* Converger and optimizer component sent value, whether the loop was converged/optimized on each iteration again (In 6.0.0 and 6.0.1, it was sent only once at the end as loop was converged/optimized) 


##### RCE 6.0.1 - Kapalua Butterfly (released at 28 January 2015)

* Fixed scheduling of script component - problem occured if it was enabled, that the script component should run on each new input value 


##### RCE 6.0.0 - Kapalua Butterfly (released at 15 January 2015)

###### Workflow Editor

* Components in a workflow can now be temporarily disabled: during workflow execution, these components and all their connections are treated as if they didn't exist
* Added an option to show the number of connections represented by a line between two components
* The connection editor dialog can now be opened by double-clicking a connection line, too
* Added shortcuts for "Draw Connection" mode (ALT + D) and "Selection" mode (ALT + S) 

###### Connection Editor 

* Improved handling of workflows with many connections
* Inputs which are already connected to an output are now marked by a small arrow
* Added more filtering options for inputs and outputs (match exactly, start with, contain) 

###### Workflow List 

* Pause/Resume/Cancel/Dispose can now be applied to multiple workflows at once 

###### Timeline GUI 

* Added new feature that visualizes when each component was running during workflow execution 

###### Configuration

* Merged all previous configuration files (*.json) into a single file (configuration.json)
* Added option to open and edit this configuration file within the GUI (Help -> Open Configuration File)
* Made example configurations and path information accessible from the GUI (Help -> Open Configuration Information)
* Moved example configuration files to the easier-to-find "examples/configuration" folder
* The "instanceName" configuration entry now supports the placeholders ${systemUser} and ${hostName} for better default naming of RCE instances
* Added "connectOnStartup" option to network connections; the old behavior was to always connect when RCE starts, which is now optional 

###### Multi-User Support  

* Added full support for multi-user operation, i.e. one RCE installation can be used by multiple users on the same system; the RCE installation directory can be set to "read-only" for all users
* Added support for multiple profiles per user, each with its own configuration, log files, and data storage
* On Linux, temporary file directories are now automatically separated (/tmp/rce-temp-<user id>) to prevent file ownership and permission problems; when customizing this path, the placeholder ${systemUser} is available 

###### Installation and Release Signing 

* Provided .deb packages for Debian/Ubuntu/Mint
* Provided .rpm packages for CentOS/Red Hat/SUSE
* Provided a signed APT repository for Linux Debian/Ubuntu/Mint which allows installation via "apt-get" or package managers with GUI (e.g. Synaptic), as well as full integration with automatic system updates
* All .zip, .deb, and .rpm release packages now have digitally signed checksum files (SHA256SUMS.asc) 

###### Linux Support 

* Completely reworked (and simplified) Linux daemon installation and handling ("rce-daemon" command)
* Improved Linux distribution compatibility; RCE is now regularly tested on Debian 7, SUSE 11 SP 2, and CentOS 7 (all 64 bit); it is also confirmed to run on Ubuntu 12 LTS, Ubuntu 14 LTS, Mint 17, and Red Hat Enterprise 6
* Added standard Linux desktop integration (application icon and basic menu entry) 

###### Remote Tool/Workflow Access 

* Added a Remote Workflow Access template, which can also be used for out-of-the box testing
* Simplified the handling of input and output directories; when accessing them from script code, no additional sub-directory levels ("/input/") are required anymore
* Moved example script files to the easier-to-find "examples/remote_access" folder 

###### Optimizer Component 

* Added an output that indicates whether gradients are requested by the optimizer method within the current iteration
* Updated integrated Dakota version from 5.1 to 6.0
* Added Dakota methods: SOGA, MOGA
* Added support for using precalculation files (reset files)
* Added support for failure-tolerant optimization loops (see Section "Workflow Engine" below) 

###### Script API 

* Various improvements and extensions (e.g. support for "not a value", execution count, persistent state)
* Added option to close the outputs of tools in pre/postprocessing scripts of tool integration
* Removed deprecated parts of the API 

###### Tool Integration 

* Made the script API accessible in pre/postprocessing
* Added an option to limit the number of parallel tool executions (e.g. to prevent excessive license usage) 

###### Data Management 

* Major rework of the data management backend (data model and API) to increase performance and robustness
* Workflow Data Browser now shows input and output data of all components and additional workflow run information
* Workflow Data Browser now also shows the path to the working directories of integrated tools (for manual inspection) 

###### Usability 

* Simplified resetting the "Don't ask again" setting of the workspace chooser. It can be reset via "Help -> Open Configuration Information". 

###### Batch mode (rce --batch) 

* Command execution output is now written to standard output as well, which makes it available from the invoking command line
* Improved robustness 

###### Workflow Engine 

* Completely reworked workflow execution (as a prerequisite for single component run, restart after failure, and stepwise execution)
* Reworked scheduling options (initial, required, optional -> constant, single, consumed and required, required if connected, not required). For details and migration path, see documentation.
* Provided better support for nested loops including reset of nested loops
* Made optimization and parametric study loops failure-tolerant, i.e. extended Python/Jython script API to allow to indicate that tool/script failed, but only because of invalid input parameters
* Added automatic writing of basic history data (inputs, outputs) to all components
* Fixed workflow console row writer to ensure no log rows are missing after workflow execution
* Added handling of system time differences between workflow controller and component
* Added handling of system time differences between workflow controller and component hosts
* Added support for limitation of parallel tool executions 

###### Cluster

* Added support for TORQUE 5.0 

###### Converger

* Added possiblity to finish on first check for negative values 

###### Commands

* Added "–dispose" flag to "wf run" command to allow disposal after workflow execution
* Added "–dispose", "–pr" and "–sr" flags to "wf verify" command (for automated testing) 

###### Other/Misc 

* Merged the three former RCE editions (Standard/CPACS/Transport) into a single one
* Improved branding (splash screen, about dialog, ...)
* Upgraded to Eclipse RCP 3.7.2
* Improved the cleanup mechanism for temporary files
* On RCE startup, the last debug.log and warnings.log files are automatically preserved as *.previous.log
* Various bugfixes
* Various usability tweaks 

