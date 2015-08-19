import java.util.jar.Manifest;

import groovy.json.JsonBuilder

def builder = new JsonBuilder()
def json = builder {}
json['bundleProperties'] = []

def keys = [
    'Bundle-SymbolicName',
    'Import-Package',
    'Export-Package',
    'Require-Bundle',
    'Fragment-Host'
]

def rootDir = new File('../../..')
rootDir.listFiles().each {
    def manifestFile = new File(it, 'META-INF/MANIFEST.MF')
    if (manifestFile.isFile()) {
        Manifest manifest = new Manifest(new FileInputStream(manifestFile))
        def attribs = manifest.getMainAttributes()
        def jsonEntry = [:]
        keys.each { key ->
            jsonEntry[key] = attribs.getValue(key)
        }
        json.bundleProperties += jsonEntry
    }
}

//println json.toPrettyString()
def jsonOut = new File('target/raw-manifest-data.json')
jsonOut.write(builder.toPrettyString())
