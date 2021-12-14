RCE 10.3.0 (December 9, 2021)

# Palette View

- Implementation of a new workflow palette with the following features:
  - By default, all components are sorted into the top-level groups 'Standard Components', 'User Integrated Tools' and 'User Integrated Workflows'
  - Groups and subgroups can be added, edited and deleted
  - Workflow components can be moved into custom groups
  - Workflow components can be reset to their default group
  - The "Manage Custom Groups" dialog allows users to organize their user-defined groups, especially when components are not available on the network 
  - Empty groups can be hidden in the palette
  - Additional component information can be displayed in a dedicated dialog
  - A filter mechanism enables the search for components 
  - The component help (F1) can be opened directly from the palette 

# System Monitoring

- Major rework of the System Monitoring (CPU/RAM usage information) feature, including fixes and performance improvements

# Network View

- Added a "Show configuration snippet" feature
  - Selecting this from the context menu of a connection shows a dialog containing an auto-generated configuration block ("snippet")
  - This snippet can copied into the main JSON configuration file to make the connection persistent
- Minor bug fixes in the Network View

# Tool Integration

- A new parameter "comment" has been added for tool properties of integrated tools in order to give the workflow designer some more information about this property
- Added export functionality for the tool documentation to the Properties view

# Workflow Components

- Design of Experiments
  - Minor gui improvements in the properties view for the method selection
  - Minor bug fixes when using the Design of Experiments component
- VampZero Initializer
  - Set the component to deprecated

# Command Console

- Added 'wf start' command to start a workflow without waiting for its termination

# Security and Third-Party Libraries

- Upgraded Apache SSHD (SSH/Uplink server port) to 2.7.0
- Upgraded Bouncy Castle (Cryptography) to 1.69
- Upgraded Cucumber (QA library) to 1.2.6
- Upgraded Ant (QA dependency) to 1.10.12

# Documentation

- Added description of setting up RCE as a systemd service to the Administrator Guide
- Moved description of setting up RCE as a Windows services from the User to the Administrator Guide
- Various improvements in the Administrator Guide, especially regarding Uplink

# Other

- Reduced the volume of debug.log messages on Uplink relay (server) instances
- Added internal property "rce.eventLogOutput" to redirect event log output to the console (value "stdout" or "stderr") or to a different file (value "file:<path>")
- Added CPU, RAM, PID, and JVM version information to the event log on startup
- Some mior fixes concerning the undo behaviour
- The validator of the Input Provider has been improved
- Fixed a bug in the runtime view of the Optimizer that resulted to NaN values when data was saved
- Minor fixes, documentation and usability improvements


RCE 10.2.4 (August 19, 2021)

# Network / Uplink

- Fixed a server-side issue that could cause lockups in low-bandwidth situations
- Reduced internal memory consumption per file transfer on relay servers
- Improved message prioritization (e.g. file transfers having lower priority than tool announcements)
- Fixed a server-side issue where client connections failing their heartbeat check were not closed correctly
- Fixed a client-side issue where tools received over an Uplink connection could sometimes be forwarded to other users in the local network even with the "Gateway" flag off; not a security issue as those tools were still only visible to users with proper authorization, and could not be executed
- Fixed a rare client-side issue where file transfers could stall indefinitely on the sender side; also reduced memory consumption
- Improved log output

# Network / Other

- Fixed SSH connections retrying to login with the same password after the first attempt fails

# Workflow Execution

- Fixed a rare issue where a local tool could be incorrectly rejected by the "allowed to execute" mechanism


RCE 10.2.3 (May 10, 2021)

# Network

- Uplink: Added network message prioritisation and improved file upload/download behavior 
  to prevent timeouts in low-bandwidth situations
- Uplink: Adjusted protocol parameters (e.g. increased timeouts) to further improve stability
- Uplink: Fixed bugs causing rare stability issues on client and server side
- Uplink: Improved client-side GUI behavior regarding connect/disconnect/retry
- Improved logging of various network events, mostly on server side
- Reduced the volume of low-level SSH events being logged

# Components

- Optimizer: Fixed a bug that prevents the generic optimizer to fail for design variables of type vector       
- Optimizer: Fixed a bug in the generic optimizer get_start_value() API command that returned None    

# Other

- Minor documentation fix regarding component versions
- Minor metadata, code, and JavaDoc cleanup


RCE 10.2.2 (Feb 04, 2021)

# Network

- Fixed a memory leak in the handling of incoming SSH/Uplink connections (present
  in RCE 10.2.1)

# Library Changes

- Upgraded Apache POI to 4.1.2 to fix a security issue
- Upgraded XMLBeans to 3.1.0 (transitive dependency)

# Documentation

- Added missing flag `--expose` in documentation of `wf integrate`


RCE 10.2.1 (Dec 08, 2020)

# Network (SSH, Uplink, and Remote Access Connections)

- Improved event.log entries: added more information, especially regarding login 
  events and opening/closing connections, and reduced the number of events;
  fixed cases where the end of a session was not always logged
- Lowered timeout of SSH connections from 10 minutes to 1 minute for technical 
  reasons; Note that this also affects interactive SSH sessions
- Set an explicit limit of three login attempts per connection (currently hardcoded)

# Library Changes

- Upgraded OSHI to version 4.9.5, fixing a memory leak in long-running RCE instances
- Removed the obsolete dom4j 1.x library

# Documentation

- Rewrote section on installation in user guide to better explain
  signature handling

# Other

- Extended validity of RCE signing key (unchanged fingerprint: 0xBA880CB39DC1CE34, 
  new expiration date: December 2, 2022)
- Reinstated commands `keytool` and `sysmon`, which were unavailable in 10.2.0 
  by accident
- Minor bugfixes


RCE 10.2.0 (Nov 11, 2020)

# Network

- Improved handling of (experimental) Uplink connections, improving robustness 
  and preventing cases where account/client id combinations could remain blocked
  after disconnecting.
- Improved reliability of the Uplink network connection GUI.
- Changed GUI behavior (for all connection types): Double-clicking a network 
  connection now triggers a connect/disconnect instead of opening the edit dialog.

# Components

- Switch: Made it possible to configure multiple inputs, outputs and conditions
- Output Writer: Added a configuration option to allow overwriting files and 
  directories if necessary
- Output Writer: Fixed validation and minor GUI bugs
- Design of Experiments: Fixed/improved validation messages

# Scripting (general) 

- Extended the Script API so that Python input parameter files can be written during 
  workflow runs ("Input File Factory"), including tool pre/post scripts.

# Python Agent

- Script: Added the first version of the experimental "Python Agent" option. Instead 
  of starting a new Python interpreter on each execution, this option starts a 
  long-running Python process that executes scripts on demand. This can significantly 
  speed up workflows that use a lot of individual scripts.

# Workflow as Component

- (experimental) Added a first version of Workflow as Component, a feature that allows 
  wrapping and publishing whole workflows as virtual components/tools. Please
  note that this feature is currently only available via console commands. GUI 
  support is planned in future releases.

# Administration

- Added the first version of a compact "event log" in the profile directory.
  This file gives a concise overview of relevant system events, for example connection
  attempts or disconnects. Currently, this is mostly useful for server instances. The
  number of events being logged will be expanded in future releases.
  
- (experimental) SSH accounts can now be edited at runtime through a separate 
  configuration which is reloaded on changes. This feature will be documented when it  
  is considered stable, as some details are still subject to change. If you want to 
  use this already, please contact us for details.

# Other

- Adapted the deprecated SSH Remote Access API to make a C/C++ library using it
  work with RCE 10.x again.
- Deleted outdated developer documentation
- Minor other bugfixes
- Minor documentation improvements


RCE 10.1.1 (July 01, 2020)

# Network

- Fixed a rare issue with Uplink connections that caused remote tool execution to fail

# Security and Third-Party Libraries

- Upgraded to Jackson 2.11.1, Bouncy Castle 1.65.01, ActiveMQ 5.3.13, 
  Apache SSHD 2.5.0, and Commons Collections 4.4

# Other

- Minor documentation fixes




RCE 10.1.0 (May 11, 2020)

# Profile Management and UI
- Added recently used profiles to the default profile selection menu
- Fixed bug in the profile selection menu that the correct versions will be displayed for recently used profiles
- Fixed bug when upgrading profiles

# Data Management
- Added warning in case of directory names that are not valid on all platforms
- Fixed bug in the output writer that the file extension will be transferred correctly

# Tool Integration
- Fixed bug in the tool execution that occured on Windows for "execution commands" that begin and end with a quotation mark
- Made integration more robust against errors in individual integrations

# Workflow Example Project
- Added example file for CPACS 3

# Monitoring
- Replaced the previously used SIGAR library with the more up-to-date OSHI library
- Updated the displayed information about RCE subprocesses to platform changes

# Miscellaneous
- Removed irrelevant log output on startup and shutdown
- Fixed minor GUI bugs
- Fixed a bug in "wf graph" console command

# Internal Improvements
- Updated the reference checkstyle version to 8.30
- Updated Maven-checkstyle-plugin to 3.1.1
- Updated Easymock to 4.2
- Improved source compatibility with recent versions of Eclipse
- Added "Automatic-Module-Name" bundle headers




RCE 10.0.0 (Sept 22, 2019)

# New Features
- Implemented Welcome Screen
- Implemented Icon Caching
- Implemented experimental SSH uplink functionality
- Implement file-based import of credentials and authorization groups
- Let user display group ID for published components in network view
- Allow users to rename logical nodes of an instance

# Documentation
- Written first draft of Administrator Guide

# Optimization
- Decreased size of serialization of WorkflowGraph
- Perform icon scaling of integrated tools at integration time instead of execution time

# Infrastructure
- Upgraded to Jackson 2,10,0, Apache SSHD 2.3.0, Commons-Compress 1.19, ActiveMQ 5.15.10, BouncyCastle 1.64, JSch 0.1.55
- Extended BDD test coverage
- Fixed installation of .deb package on Ubuntu systems
- Fixed declared dependencies of .deb package

# Miscellaneous
- Fixed typo in logging message of InputLoader
- Improved error messages in XPathLoader
- Improved parsing of XML files by XML Loader
- Improved user feedback on headless servers
- Set command line flag for profile upgrade to `--upgrade-profile`
- Prevented bursts of background task execution after suspending and resuming