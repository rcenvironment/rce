##### RCE 7.1.5 - Blue Cobra (released at 10 November 2016)
* Fixed a bug that caused integrated tools with a dot in their name not to open their tool documentation correctly.
* Fixed a bug in the Workflow Data Browser where workflow data was shown incorrect under some rare circumstances.

##### RCE 7.1.4 - Blue Cobra (released at 28 September 2016)
* Fixed a bug with very large float entries in data type Smalltable, Vector and Matrix. It could make workflows hang indefinitely.
* Fixed a value overflow in data type Integer
* Fixed a bug in the Design of Experiments component when loading custom tables. When using scientific notation the first entry was misinterpreted.

##### RCE 7.1.3 - Blue Cobra (released at 31 August 2016)
**Note: We highly recommend to update to RCE 7.1.3. In prior versions a bug could make your workflow unusable (see first bullet for details)**

* Fixed a bug in the workflow file encoding mechanism. It caused workflow files to grow until they couldn't be opened anymore. This happened when workflow labels contained special characters like umlauts.
* Fixed a bug in Parametric Study component's runtime view that prevented forwarding values from being shown

##### RCE 7.1.2 - Blue Cobra (released at 21 June 2016)

* Fixed Optimizer component's runtime/results view and some internal problems in the Optimizer component
* Fixed some problems with external file references in workflows (relevant for scripts in Script and Cluster component and for mapping files in XML Merger component)
* Few other minor fixes

##### RCE 7.1.1 - Blue Cobra (released at 03 June 2016)

* Fixed internal problem which prevented the RCE startup on Windows Server 2012 R2 (technical background: dependency of target platform bundle could not be resolved)
* Removed timeout when starting a component (technical background: the timeout caused problems with long running tool copy operations) and increased some other timeouts
* Some minor fixes

##### RCE 7.1.0 - Blue Cobra (released at 24 May 2016)

###### Synopsis

* Workflow Editor: Fixed a bug in copy&paste of components that caused connections between components to malfunction
* Made documentation available from within RCE
* Added a navigable thumbnail view of the workflow editor
* Improved robustness (esp. during workflow execution and loading data from data management)
* Improved handling and capabilities regarding fault-tolerant loops
* Various bugfixes and improvements

###### Workflow Editor

* Fixed a bug in copy&paste of components that caused connections between components to malfunction
* Unified zooming, scrolling and panning (Ctrl + mouse wheel or Ctrl + '+'/'-' for zooming, mouse wheel for scrolling, Space for panning)
* Slightly altered size of components and grid to enhance symmetric layouts
* Added possibility to make bendpoints of custom connection paths snap to the grid

###### Workflow Components

* Cluster: Added capability to cancel execution
* Converger: Added possibility to define behavior if convergence is not reached within check limit (Ignore, fail or send 'not-a-value')
* Converger: Added per-input convergence indicator output
* Database: Enhanced handling of different result set entry types
* Evaluation Memory: Added support for failure tolerant loops
* Evaluation Memory: Added capability to store failure results
* Evaluation Memory: Added capability to use precalculation file
* Optimizer: Removed usage of precalculation file as Evaluation Memory component provides this functionality
* Output Writer: Added new placeholders "Original filename" and "Execution count" for target file or director
* Script: Added support for data type "Matrix"
* Script: Execution can be cancelled during script evaluation
* XML Merger: Added functionality to receive the mapping file as an input
* XML Merger: Fixed a bug when simultaneously editing mapping files in different XML merger components

###### Workflow Data Browser

* Fixed data export in case of multiple simultaneous exports
* Improved robustness when loading directories

###### Tool Integration

* Added capability to revoke documentation of integrated tools
* When cancelling the execution of an integrated tool the related native processes are terminated now
* Fixed execution location (tool or working directory) of pre and post execution scripts
* CPACS tools: Added capability to write 'not-a-value' as CPACS result file

###### GUI Misc

* Made documentation (User Guide, Components Help, Tool Integration Help, Developer Guide) available from within RCE (Help -> Help Contents)
* Added a navigable thumbnail view (named Outline) of the workflow editor (if it's not present open it via Window -> Show View -> Outline)
* Made "Execute Workflow" accessible via "Run" menu
* Made "Advanced" tab in Properties view read-only
* Fixed entries in the Log view which got mixed up between instances in certain cases
* Fixed button activation the in Network view
* Fixed a bug that caused the Network view to be missing on Windows 8.1 and Windows 10

###### Workflow Execution

* Made workflow loading more robust against parsing errors
* Fixed data type conversion if data of different but convertible data types is exchanged between workflow components
* Added capability to distinguish between failure types in fault-tolerant loops (component crashed vs. component couldn't calculate result due to inputs received) and to define behavior for both cases individually
* Fixed storing of previously selected target instances when executing a workflow

###### Remote Access

* Added possibility to use key file authentication for SSH Remote Access
* Published workflows are now also usable in RCE instances connected via SSH Remote Access
* Improved the behavior when the internal workflow template was outdated by component changes

###### Documentation

* Various minor improvements in documentation (User Guide, Components Help, Tool Integration Help, Developer Guide)
* Added documentation for fault-tolerant loops

###### Internal

* Set a limit for total thread usage
* Reduced thread usage when executing workflows
* Fixed parallel execution of certain background tasks
* Improved performance of certain bulk operations (e.g. console output handling)
* Improved maintainability of the workflow engine
* Various minor bugfixes and improvements


##### RCE 7.0.2 - Blue Cobra (released at 03 March 2016)

* Fixed target instance selection that was broken after workflow update; issue directly affects tool execution via remote access (again, a tool is executed on specified target instance even if it is available on multiple instances in the network)
* Fixed minor resource leaks in network layer (one was caused by 'long-running' connections having a considerable amount of traffic, the other was caused by connection attempts that failed because of version mismatch)
* Fixed storing of directories in the data management that contain many files (issue affects also the transfer of directories that contain many files)
* Increased fault-tolerance in case a deletion behavior for the working directory of an integrated tool is selected that is not supported anymore by the integrated tool

##### RCE 7.0.1 - Blue Cobra (released at 01 December 2015)

* Fixed uploading of files and directories to remote RCE instances (users that run RCE 7.0.0 in a distributed RCE network should upgrade to 7.0.1 as soon as possible)
* Fixed publishing of workflows (to make them available via remote access)
* Fixed determining the optimal design values in the Optimizer

##### RCE 7.0.0 - Blue Cobra (released at 25 November 2015)

###### Synopsis

* New workflow components: Database, Evaluation Memory
* Remote tool access via SSH connections (secure way to execute tools across untrusted networks)
* Improved network communication system (scalability, generated system load)
* Advances in fault-tolerant and (multi-)nested loops
* Various bugfixes and improvements 


###### Workflow Examples Project

* Reworked structure of example workflows to function as a kind of tutorial

###### Workflow Execution

* Enhanced Workflow Execution Wizard
  * Added ability to assign RCE instances to multiple selected workflow components in one step
  * Added ability to assign any remote RCE instance randomly
  * Added ability to group the table with the workflow components by workflow component type
  * Improved filter options
  * Added full-text search for the table with the workflow components
  * Prevent from closing wizard immediately after opened in highly distributed environments
* Reviewed and improved all workflow-related log messages
* Reworked nested-loop capability so that multi-nested loops are possible (introduced more restrict rules which are explained in the user guide and also in the workflow examples)
* Extended the capability of fault-tolerant loops: each driver workflow component (Optimizer, Design of Experiments, Converger, Parametric Study) is able to define the kind of fault-tolerance of the loop driven

###### Workflow Components

* Output Writer supports simple data types (values of type float, integer, short text, etc. can be written in a file with a user-defined format)
* Optimizer writes results in a csv file and stores it in the data management
* Added new workflow component 'Evaluation Memory' which is able to store evaluation results for later re-use
* Design of Experiments, Converger, and Parametric Study are now nested-loop capable
* Added workflow component "Database" which supports select, insert, update, and delete statements for MySQL and PostgreSQL databases
* Made cancelling of Optimizer more robust
* Set SQL Reader, SQL Writer and SQL Command to deprecated
* Eliminated deprecated CPACS tool wrapper
* Cluster component supports relative paths
* Converger: fixed incrementing number of convergence checks for multiple inputs  
* Workflow driver components (Optimizer, Design of Experiments, Converger, Parametric Study) are able to forward values 

###### Workflow Editor

* Extended validation for workflow components: a validation error is shown if an input is not connected that is declared as required
* Improved routing so that labels do not interfere with connection lines anymore

###### Workflow Files

* Made the textual representation of workflow files more steady on workflow modifications
* Made parsing of workflow files more robust against modified integrated tools (workflow is still opened, but the affected tool is left out)

###### Workflow Data Browser

* Log messages of workflow components are completely available in the workflow data browser
* Error log messages are aggregated for a workflow in one single log file which is available in the workflow data browser
* The workflow file for each workflow executed is available in the workflow data browser
* XPaths of dynamic inputs/outputs of XML and CPACS workflow components are shown in the workflow data browser
* Changed default for storing workflow data (formerly: history data): they are stored by default for all workflow components now (can still be disabled)

###### Tool Integration

* Added support for user documentation (one single PDF or *.txt file; max 50 MB)
* Added a so called "Tool run imitation mode": kind of a dummy behavior can be defined for integrated tools that imitates the actual tool run (useful to test workflow logic or debug certain tools in a loop)
* Copying of integrated tools is always done sequentially (to avoid I/O load peaks at workflow start if many tools of a workflow need to be copied)
* Default working directory of tools is not the RCE's temp directory anymore (as this results in using the RCE's temp directory even if the working directories are used for inspection during and after workflow execution)
* Default handling and default execution constraints can be defined for inputs by the tool integrator when integrating the tool

###### Console Commands

* Added command "wf delete" that deletes a certain workflow from the data management

###### Maintenance/Administration

* Improved log output: added additional relevant information, and removed irrelevant messages
* Added correlation markers ("E#...") to connect observed errors on one machine to root causes that happened on another
Linux .deb packages do not strictly require the OpenJDK JRE anymore (although this is still the tested and recommended option)
* Added a startup option to run a headless instance without local components (--disable-components)
* Addded system monitoring support that makes system information (like global RAM and CPU usage) available even for remote RCE instances (if enabled)

###### Performance

* Improved scalability of the network communication system, and reduced the system load generated by it. This increases the maximum possible number of clients per relay, increases message throughput, and improves stability when there is high external CPU load on the system (e.g. from running integrated tools on the same machine).
* Reduced network traffic by reducing and combining individual messages.
* Improved performance of the database used to store all of the workflow data.

###### Security

* Passwords for SSH accounts are no longer configured as plain text, but as a non-recoverable ("hashed") format instead. Plain-text configuration still works, but is discouraged; a warning will be logged on startup.
* As the new password format cannot be generated manually, a special configuration interface for SSH accounts has been added ("rce --configure"). To support editing SSH accounts on servers, this UI can also be run in text mode.
* As a precaution, RCE was hardened against a common network vulnerability. Although RCE is intended to be run in trusted networks only, users are still encouraged to upgrade to RCE 7.x as soon as possible.

###### Remote Access

* SSH connections to other RCE instances can now be added in the network view
* Tools that are published with the remote access template can now be used by remote instances via SSH connections
* Increased the range of valid remote tool and workflow ids (names) and versions
* Potentially risky characters are now prevented from being used in parameter strings (for example the backslash, or double quotes)

###### Others/Misc

* Fixed that on some Linux distributions clicking on web links in the help view or in PDF files caused the JVM to crash
* The workflow list view shows workflow state icons

###### Internal

* Various bugfixes and improvements
* Improved database update methods, guarded against opening a database that was created with a future version of RCE 
* Global lock for XML mapping to avoid memory peaks if big XML files are processed
* Updated TiGL Viewer binaries to version 2.1.6
* Guarded the profile directory against being used with older RCE versions
* Reworked start up validation and pre-workflow execution code to make it more maintainable
* Improved handling of endpoint data of type 'Small Table' (workflow data browser, workflow console, etc.)
* Improvements in workflow engine: robustness, failure handling 

