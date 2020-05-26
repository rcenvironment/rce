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