To run the tests, download RCPTT (for developing the tests, RCPTT 2.0 was used - https://www.eclipse.org/rcptt/download/).
It is recommended to use a separate instance to test RCE with RCPTT.
In the Application tab in the bottom add RCE. Therefore select RCE's root folder. You can edit the configuration with Right-click -> Configure... -> Advanced...
Using the known mechanism add the test cases and contexts to a RCP Testing Tool Project (Contexts are preconditions to tests, such as resetting the perspectives)
The tests having the "_" prefix are known to fail currently (e.g. because the Wf Updater is triggered).
Tests that depend on coordinates, such as dragging of components, dragging of bendpoints, pulling connections are prone to failing on small resolutions.
Run the "all.suite" to run all tests at once.

For the time being, some paths are hardcoded and have to be adapted accordingly:
- Context_NoCommonToolsIntegrated: The Root path must point to the folder where integrated tools are stored(".../tools/common", e.g. "file://D:/rcptt-profile/default/integration/tools/common").
- Context_CleanDataManagement: The Root path must point to the folder where the data management is stored (".../storage/", e.g. "file://D:/rcptt-profile/default/storage/").
- In the run configuration of the AUT you can define the profile's parent directory. In the "Arguments" tab under "VM arguments:" add the following line and adapt it as you like:
	-Drce.profiles.parentDir=D:/rcptt-profile
- In configuration "Use Configuration file" set the dev_config.ini file that is located next to this README file