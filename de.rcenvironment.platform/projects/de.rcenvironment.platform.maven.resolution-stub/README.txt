This Maven project is contained in a subfolder, instead of being a
top-level project, to avoid runtime complications from the Eclipse IDE 
recognizing it as a bundle project.

The purpose if this bundle is to satisfy the Tycho platform resolution on
OSGi bundles loaded from Maven repositories (especially, the PAX bundles).
Without it, the Import-package resolution fails. It is only needed for the
platform generation, and is not deployed as part of the generated platform
repository. In fact, it MUST NOT be part of the platform used at runtime,
as the Export-Package declarations are "empty", ie no actual classes are
provided. If this bundle was contained in the OSGi runtime environment,
imports on the exported packages (like org.slf4j) could be wired to this
bundle, causing ClassNotFoundExceptions in the importing bundle.

  - R. Mischke, Feb 2012

  
  Moved up to "projects" folder because platform projects are not automatically
 imported in Eclipse anymore. The resolution stub can now also export other maven 
 bundles (e.g. the org.jcraft.jsch bundle).
 
 	- B. Boden, Feb 2018 