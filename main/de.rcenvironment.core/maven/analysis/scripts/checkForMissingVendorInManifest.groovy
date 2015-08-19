/**
 * Parses all Manifest files and checks if 'Bundle-Vendor' is declared.
 * Prints all bundles to the console, which misses the 'Bundle-Vendor'.
 * Apply the root dir to your environmont, if you like to use it for other repositories than RCE standard.
 * 
 * Exit code 1 if at least one Manifest file misses declaration, 0 otherwise.
 */

import java.util.jar.Manifest;

def missed = false
println 'Bundles with missing \'Bundle-Vendor\' declaration:'
def rootDir = new File('../../../..')
rootDir.listFiles().each {
    def manifestFile = new File(it, 'META-INF/MANIFEST.MF')
    if (manifestFile.isFile()) {
        Manifest manifest = new Manifest(new FileInputStream(manifestFile))
        def attribs = manifest.getMainAttributes()
        if (attribs.getValue('Bundle-Vendor') == null) {
            println attribs.getValue('Bundle-SymbolicName')
            missed = true
        }
    }
}
if (missed) {
    System.exit(1)
} else {
    println '- none -'
}
