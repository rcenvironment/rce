##### RCE 3.2.0 - Honolulu Juicer (released at 24 September 2013)

* improved handling of long-lasting remote component initializations
* fixed validation errors not showing up in log
* fixed connection removal via Undo
* workflows are properly disposed on shutdown now
* improved default workspace locations of "launch" configurations
* fixed double-click in read-only workflow view switching to properties view
* changed behaviour of Script component on second click
* updated example workflows and fixed nested loop workflow
* fixed sending an output to more than one input
* guarded converger component against certain loop behavior
* fixed handling of Boolean values in component input view
* existing workflows are now properly shown after a network merge
* workflow console output is now properly removed after workflow deletion
* fixed remote workflow deletion breaking workflow tabs
* fixed workflow console scrolling issues
* made TiGLViewer a dedicated component
* TiglViewer: fixed issue with workbench restart
* Optimizer component: improved configuration GUI
* Python/Jython component: deprecated old script API
* Python/Jython component: using the same names for inputs and outputs is now allowed
* Python/Jython component: fixed hangs on long console output
 * Excel component: fixed pruning of output channel
* minor usability improvements
* improved test code coverage
* various other bug fixes 


##### RCE 3.1.0 - Honolulu Juicer (released at 27 August 2013)

* Fixed bug regarding boolean values sent within a workflow (workflow components always retrieved 'false' even the previous component sent 'true')
* Added support for workflow control from within scripts executed with the Script component again
* GUI misc: mark GUI elements red if validation error exists (instead of yellow which is meant for validation warnings)
* internal: added example workflow components demonstrating how to develop a RCE workflow component 


##### RCE 3.0.0 - Honolulu Juicer (released at 2 August 2013)

* Added support for remote upload of large files
* Unified data types of input values and output values which are used by workflow components (Note: all workflow files created with RCE < 3.0.0 will be updated by RCE when opening them for the first time)
* Replace the Python workflow component with a more generic Script workflow component which supports still Python and and latterly also Jython
* Added check if two RCE instances are compatible to each other (connection between them will be refused if not compatible)
* Added support for maximum iteration count in Converger workflow component
* Reduced messages exchanged during workflow run
* Improved GUI
	- added icon in workflow runtime tab, which shows the current state of a workflow
	- Fixed memory leak in tab showing the input values of running workflow components
	- Reworked and improved most of the workflow component configuration GUIs
	- Improved the workflow run editor: it only allows RCE nodes to be selected which has the workflow component with an exact matching version installed
	- Added check for invalid connections: if data type of one connection's endpoint (input/output) is changed it will be checked now if it is compatible with the data type of the other endpoint
	- Made the GUI in general more responsive even if long running (remote) operations are performed in the underlying backend
	- More minor improvements 

