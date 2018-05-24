This is an extension to run scripted BDD-style tests from the RCE command console.

Currently, some required libraries are not provided through the RCE platform, which 
means that after a fresh checkout, these packages will show compile errors. To fix
these, run a local Maven build of the extension using the .launch files in 
/de.rcenvironment.extensions.testscriptrunner/eclipse/build/ and refresh the 
projects in your IDE. This will make the required Maven dependencies available in
the local classpath.
