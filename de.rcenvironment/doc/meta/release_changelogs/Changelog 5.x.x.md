##### RCE 5.2.1 - Springtime Cooler (released at 29 August 2014)

* Fixed error when integrating a tool using a template of type 'Common'
 

##### RCE 5.2.0 - Springtime Cooler (released at 25 August 2014)

###### General

* Improved clean up of RCE's temp directory 

###### GUI 

* General performance improvement in workflow data browser
* Fixed handling of scripts that were openend in an editor at the same time and that belong to different script components 

###### Workflow components 

* Optimizer component
	- Improved behavior if optimizer fails or isn't available at all 
* Script component
	- Improved error handling if non-existing files are written to an output 

###### Tool integration 

* Fixed bug using boolean values in pre/post script
* Added support for relative paths to the tool directory (relative to the RCE installation directory) 

###### Network

* Fixed a bug where many crashed connections could cause a high number of remaining threads and could also cause a memory leak 

###### Headless RCE 

* Added new command for batch execution. It executes a set of commands and shuts down RCE no matter what: --batch <command string>
* Added support for placeholder values on component instance base (and not only on component type base)
* Improved fault tolerance when execution a workflow in batch mode and not all of the required remote nodes exists immediately on start up because of network latency (but will be available directly after start up) 

###### Documentation

* Improved documentation of string security filter
* Added documentation for placeholder value files used in headless mode 


##### RCE 5.1.0 - Springtime Cooler (released at 25 July 2014)

###### GUI

* Added a view to execute RCE console commands
* Added miscellaneous context menu entries for faster access to key features (such as integrating a new tool or creating the workflow examples project)
* Fixed short cut for renaming workflow nodes (it is just F2 now)
* Improved visibility of self-connections of components in the workflow editor
* Component names are shortened from the center if the entire name is too long to fit in the workflow node box
* Further improved the GUI's responsiveness, especially when RCE is connected to remote instances
* Placeholders (such as Python installation path, cluster component credentials) are not shared between copied workflow files anymore
* Various bug fixes 

###### Workflow components 

* Cluster component 
	- Absolute paths to the cluster queuing system commands are now configurable 
	- Added support for indicating failed cluster jobs 
* Converger component 
	- The number of iterations to consider in the convergence check is now configurable (not only the current and previous, but the current and n previous values can be considered now)  
* Optimizer component  
	- Starts dispatching initial design variable values for evaluation not before it gets the first explicit evaluation request from the optimization algorithm 

###### Tool integration 

* Tools can be published and unpublished on the file-system level without restarting RCE (especially useful for non-GUI server nodes) 
* Miscellaneous improvements 

###### Command interface 

* The "wf run" command can now be used to run workflow files containing placeholders (Python path etc.) as well; this is done by specifying a JSON file containing the required placeholder values
* Added the "wf verify" command for batch testing of workflow files; placeholder files are supported as with the "wf run" command 

###### Administration 

* When installing RCE as a Linux daemon, the RCE instance can now be run as a non-root user (which is highly recommended); note that the installation process still requires root permissions
* Added a startup check to detect missing temporary folder permissions early 

###### Network

* Active connection attempts can now be cancelled by pressing "stop/disconnect" in the Network View, or by using the "cn stop" console command
* Failed connections waiting to auto-reconnect can now be made to reconnect immediately (without stopping them first) by pressing "start/connect" in the Network View, or by using the "cn stop" console command 

###### Remote access / Tool Access 

* In addition to the remote execution of single tools, complete workflows can now be invoked through the SSH/SCP interface 

###### Documentation

* Added contextual help for all workflow components
* Removed the standard Eclipse platform's help content from the integrated help center and replaced it with information about RCE's user guide
* Extended the user guide with help for integrating tools in RCE 


##### RCE 5.0.2 - Springtime Cooler (released at 26 June 2014)

* Some fixes regarding the optimizer component 
* Minor fixes 


##### RCE 5.0.1 - Springtime Cooler (released at 12 June 2014)

* Some fixes regarding the deletion of workflows
* Some fixes regarding connections (auto-connect, copy&paste, ...)
* Provided dynamic help in script component again 


##### RCE 5.0.0 - Springtime Cooler (released at 15 May 2014)

###### Workflow components 

* Added an "Output Writer" component to write directories and files to the local file system 
* Optimizer
	- The iteration count is now available as an output
	- Added support for the Vector data type (to support multiple (hundreds) of design variables)
	- Added support for start values and optimal solution outputs
	- Gradients variables can be set as goals in Dakota
	- Added support for nested loops 
* Script
	- Added support for Vector and Matrix data types
	- Added support for history data (shown in the workflow data browser)
	- Fixed memory leaks related to frequent script execution 
* Tool integration
	- Added support for history data (shown in the workflow data browser)
	- Added support for hot deployment of configuration.json files (i.e., if a new tool configuration is dropped at the proper place, the tool will be considered and integrated immediately without any further user interaction. Especially useful for RCE server instances without a GUI)
	- Bugfix: All console output is now properly separated in the workflow console view 
* Renamed component: Merger -> Joiner
* Replaced/improved some component icons
* Simplified the identifiers of components (the ones which are used in the .json files to configure which of the components are published)
* Improved the ToolAccess interface that allows external (non-RCE) clients to execute integrated tools via SSH/SCP 
* Various bugfixes and performance improvements 

###### GUI

* Improved the responsivness of the graphical user interface, especially in distributed setups under high load
* Workflow editor
	- Connections are now properly handled when workflow nodes are copied and pasted
	- Introduced different sizes and shapes for workflow components in the editor
	- Removed the "Advanced" configuration tab as it is not intended (and therefore confusing) for end users
        Added support for a "point&click" method to connect component's endpoints (in addition to the existing "drag&drop" method) 
* Workflow data browser
 	- The storage location of history data is now visible in the browser
 	- Component history entries now use the component's icon for easier recognition
	- Sorting is now context sensitive (e.g., no support for alphabetical sortinng in timeline subtree)
	- When exporting, all history data is now properly fetched recursively, even if it was not made visible before
	- Updated the internal history data format for better long-term stability 
* Improved the iteration counter for components in the workflow execution view 
* Various graphical user interface improvements 

###### Documentation

* Added information about supported operating systems
* Added information about the scheduling parameters: required, initial, optional
* Added documentation of the ToolAccess feature
* General improvements 

###### Configuration and administration  

* Switched to Java 7 (i.e., Java 7 or higher is required to execute RCE 5.0.0 and above)
* Added a separate log file that only lists warnings and errors (warnings.log)
* The location of the instance data directory (including the data management's storage) can now be freely configured; it is no longer restricted to be inside "<user home>/.rce" 

