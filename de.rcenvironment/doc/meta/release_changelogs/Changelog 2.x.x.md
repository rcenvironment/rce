##### RCE 2.5.2 (released at 21 June 2013)

* fixed problem in Excel component: it occurred if the Excel component was run in a distributed scenario 

##### RCE 2.5.1 (released at 10 June 2013)

* extended Converger component to add support for nested loops
* added workflow status icons to workflow runtime tab
* added support for macros to Excel component's export function
* Excel component exports to clipboard with Excel-like insertion
* fixed copying a workflow component in the workflow editor (the copied component had the same name as the orgin one)
* fixed bug with runtime variables, which causes errors under some special circumstances
* fixed some memory leaks
* fixed start up validators
* added validation to the workflow connection editor: it prohibts a connection between an input and more than one output (this would result in scheduling problems)
* minor fixes 

##### RCE 2.5.0 (released at 5 April 2013)

* A new component for submitting cluster jobs was added
* Major rework of the optimizer component
	- Updating a pre 2.5.0 workflow file containing an optimizer component will remove all previous method configuration 
* improved workflow updater
* improved logging for failing python component
* improved error handling in optimizer component
* added data type array to merger component
* passphrases for runtime configurations are stored in secure storage
* minor GUI improvement and fixes 

##### RCE 2.4.2 (released at 13 March 2013)

* added new workflow component "Input Provider": user can define input values for other components, like text, numbers, or files
* improved workflow editor handling
	- improved support for short cuts
	- fixed minor issues 
* improved logging of workflow status during execution
* improved workflow execution in batch mode
	- improved performance
	- enabled workflow execution from the command line 
* improved error handling in Simple Wrapper component
* reworked and extended RCE console commands (e.g., added one for version information) 

##### RCE 2.4.1 (released at 23 January 2013)

* added boolean channeltype in Python component
* added mechanism to support wildcard entries in publishedComponents configuration
* added configuration for rce-tmp folder
* fixed various bugs in SimpleWrapper component (i.e. regarding the use of input channels in pre/post process)
* fixed bug in converger
* minor fixes and improvements 

##### RCE 2.4.0 (released at 17 December 2012)

* Major rework of the network/communication system; changes include:
	- significant performance improvements
	- simplified setup of ad-hoc networks; in typical use cases, clients only a single line of configuration to connect to a network
	- simplified handling of firewalled systems and subnets 
* reduced supported languages to English to avoid language mixups with third-party software
* added sample project with example workflows
* improved validation of Excel component configuration
* improved error handling in Python component if an unsupported Python installation was chosen
* improvements in graphical user interface of RCE (zooming behavior, graph plotting in Optimizer and Parametric Study component, etc.)
* cleaned up graphical user interface on very first start of RCE
* fixed "Simple Wrapper" execution under Linux when using multi-line command configurations created on Windows
* fixed handling of multiple arrays in Python component 

##### RCE 2.3.8 (released at 13 November 2012)

* add updating mechanism for workflow files containing at least one Python component (it is not upwards compatible)
* fixed bug in Simple Wrapper component (occurred if used as first component within an workflow)
* fixed bug in Python component regarding array inputs
* fixed bug in SQL component regarding array inputs 

##### RCE 2.3.7 (released at 7 November 2012)

* added new concept for sensitive workflow configuration values
* re-designed Excel workflow component for stability and usability purposes
* added support for Optimizer component (Dakota) in Linux versions
* added view for cluster job monitoring
* fixed bug in configuration GUI of Simple Wrapper component (occured only in Linux versions)
* fixed bug in input inspection view, which caused the GUI to freeze
* added sorting in various component listings like workflow execution wizard, connection dialog, input inspection view
* improved temp file handling in worklfow components to use less memory 

##### RCE 2.3.6 (released at 18 October 2012)

* added option to create a workflow examples project (new -> other -> RCE)
* fixed problems and inaccurate problem descriptions in optimizer component
* fixed some gui issues in optimizer component
* fixed minor issues 

##### RCE 2.3.5 (released at 13 September 2012)

* fixed bug that caused an error in running a distributed workflow
* fixed bug in merger component
* fixed bug in merger and converger components gui
* workflow data browser nodes can now be deleted
* removed context menu entries for sorting of entries in workflow data browser
* fixed minor issues 

##### RCE 2.3.4 (released at 10 August 2012)

* added Design of Experiments component (test state)
* added sorting order for workflow data browser
* added toggle buttons for workflow data browser
* added default explaining script in python component
* added storage for persistent settings
* textfields for commands in sql and simplewrapper component now resize when view gets resized
* connection lines now use anti-aliasing
* fixed minor issues 

##### RCE 2.3.3 (released at 4 July 2012)

* added Converger component (test state)
* added Merger component (test state)
* added support for compression data management
* added filter for workflow components in connection editor
* added support for Coliny Cobyla algorithm in optimizer component
* fixed input queue view showing pending inputs of components (shows input in wrong order if a lot of inputs were sent within a short time)
* fixed deleting in- and output variables in Excel component
* fixed minor issues 

##### RCE 2.3.2 (released at 14 June 2012)

* added support for RCE headless mode: workflow execution without GUI
* added workspace chooser at RCE start up
* improved workflow scheduler logic: support of different input usages (required, initial, optional)
* added compare function for text files in workflow data browser
* added "Reset search" button for workflow console filter
* added "Save as ..." to 'File' menu
* extended start up validators: length of parent file system path, JVM settings
* fixed copying of workflow nodes: copied workflow node is a real new clone now. Thus, workflow nodes don't receive configuration values of other nodes anymore)
* fixed minor issues 

##### RCE 2.3.1 (released at 14 May 2012)

* added support for sorting in workflow data management browser
* added support for data export to Excel in parametric study component
* unified configuration tabs in parametric and optimizer runtime graphical user interface
* restored "goal" option when editing variable in optimizer component configuration
* fixed start up validator for JSON configuration files
* fixed minor build infrastructure issues 

##### RCE 2.3.0 (released at 19 April 2012)

* added optimizer component (currently for Windows only)
 	- Black-box optimization with Dakota
	- Currently supported algorithms: Quasi-Newton method, HOPSPACK Asynch Pattern Search, Coliny Evolutionary method (all of them single- and multi-objective) 
* improved "simple wrapper" component
	- added Jython as pre- and post-processing scripting language
	- added support for OS-independent and multiple commands 
* graphical user interface improvements
	- added shortcuts for copy, cut and paste (Crtl+C, Crtl+X, Ctrl+V) in Python editor and text areas
 	- made several operations asynchronous to improve responsiveness
	- added zoom to workflow editor
 	- added editing of workflow component input values
	- added information about number of component runs in mouse-over of workflow component
	- Python installation path is now reset properly after moving a workflow to another platform
	- added integrated help for components
	- added more user-friendly "properties" tab for parametric study component
	- improved Python component usage
	- added "Profiles" properties tab to enable switching between workflow component properties sets
	- removed verbose log output after update 
* raised timeouts to support long running communication between RCE instances
* improved the build/release/update process
	- made it possible to provide updates for third-party libraries
	- improved versioning to reduce update size
	- regrouped installable feature sets (for example, component groups)
	- improved p2 metadata generation
	- added support for independent RCE editions 

##### RCE 2.2.5 (released at 8 February 2012)

* added Simple Wrapper component (0006339, 0007079)
* fixed Excel component: refresh formula after insertion of data (0007033)
* fixed Excel component: export to Excel(0007034)
* fixed list of platforms involved in a workflow: add the controller platform even no component is running there (0006989, 0006932)
* improved workflow data browser: add "collapse all" icon to (0006762)
* improved logging in data management catalog backend (0006988)
* improved logging in communication layer (0006988)
* improved ComponentInstanceDescriptor with method: getInComponentContextInvolvedPlatforms (0006942)
* improved workflow editor: add grouping to component list (0007078)
* improved Pioneer component to serve as a showcase for new GUI features (0006961, 0007029, 0006855)
* improved SQL command component icon (0007022) 

##### RCE 2.2.4 (released at 4 January 2012)

* fixed unnecessary undo/redo steps in workflow editor (0006897)
* fixed performance issues when connecting to multiple servers (0006904, 0006900)
* fixed crash issue when running distributed workflows (0006895)
* fixed output channel of Python component firing when it is not assigned in the script (0006921)
* fixed minor workflow-related UI bugs (0006901, 0006553)
* fixed issue with transport of workflow information from server to client (0006919)
* improved logging and debug output (0006898, 0006920, 0006868)
* custom configurations can now be embedded by the product build (0006939)
* workflows with invalid components can now be opened in the editor (0006839)
* improved "cancel" icon (0006929)
* removed obsolete "edit additional information" feature (0006775)
* internal fixes and enhancements (0006902, 0006810) 

##### RCE 2.2.3 (released at 7 December 2011)

* added automatic IP address detection for RCE clients, with fallback option (0006786, 0006843)
* added support for placeholders and properties in JSON configuration files (0006818)
* internal fixes and enhancements (0006849, 0006687) 

##### RCE 2.2.2 (released at 15 November 2011)

* added support for xlsx files in Excel (0006760)
* added jars related to apache.poi (0006759)
* added common code base for tool wrappers (0006640)
* added new RCE event log API (0006396)
* added test libraries to target platform (0006777)
* fixed: no dirty flag on changes when opening connection manager with context menu (0006606)
* improved performance of workflow data browser (0006626)
* improved performance for displaying input of remote workflows (0006647)
* improved Excel component user feedback for doubleclick events in runtime-view (0006658)
* internal changes and fixes (0006774, 0006776, 0006756, 0006662,0006524, 0006676, 0006648) 
