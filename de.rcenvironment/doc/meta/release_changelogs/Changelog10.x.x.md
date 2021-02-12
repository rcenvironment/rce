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