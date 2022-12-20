#!/usr/bin/env python3

# Copyright 2022 DLR, Germany
#
# SPDX-License-Identifier: EPL-1.0
#
# https://rcenvironment.de/
#
# Author: Robert Mischke
import argparse
import os
import re

# enable this for extra debug output, e.g., printing the list of examined projects for validation
import sys

"""
This script validates the containing repository, and in particular the Java/OSGi projects in it, against custom rules 
and conventions not addressed by standard mechanisms like Checkstyle or Eclipse's built-in validations.

It is intended to be run manually when needed, and in particular after adding new projects; ideally, before committing
them. It is also intended to run in CI as part of quality checks. For this purpose, this script returns a non-zero 
exit code if there are validations that should not be present in quality-gated branches. The latter is also the
definition an "error" message, in contrast to a "warning".


TODO document usage
TODO define and document exit codes
TODO add unit tests (run on test projects, compare output)
TODO add CLI parameter to run on custom project path(s), e.g. for plugins
TODO add CLI parameter to enable addition projects that only make sense on clean checkouts (e.g. ".settings")
"""

VERBOSE_OUTPUT = False

# relative path from this script file's directory to the base directory for the POM paths below
RELATIVE_PATH_TO_REPOSITORY_ROOT = '../../..'


class JavaManifestParser:
    """
    A crude, but sufficient parser for Java/OSGi manifests, typically run on 'META-INF/MANIFEST.MF' files.
    """

    @staticmethod
    def parse_file(file_path: str, project_name: str):
        entries = {}
        in_attribute_block = False
        last_key = None
        with open(file_path, 'r') as f:
            for line in f.readlines():
                line = line.rstrip()  # leading spaces are relevant
                if line == "":
                    continue

                if not in_attribute_block or not line.startswith(' '):
                    # finish previous attribute, if present
                    if last_key:
                        if last_key in entries:
                            raise Exception(project_name + ' - Duplicate manifest key ' + last_key)
                        entries[last_key] = last_value

                    # start new attribute
                    if ':' not in line:  # do not check for space and value as it may be a key-only line
                        raise Exception(project_name + ' - Malformed manifest line: ' + line)
                    (last_key, last_value) = line.split(':', maxsplit=1)
                    last_value = last_value.lstrip()
                    in_attribute_block = True
                else:
                    # continue multi-line attribute
                    last_value += line.lstrip()

            # finish the last attribute, if present
            if last_key:
                if last_key in entries:
                    raise Exception(project_name + ' - Duplicate manifest key ' + last_key)
                entries[last_key] = last_value
        return entries


class OutputCollector:

    def __init__(self, include_project_name_in_output=True):
        self.include_project_name_in_output = include_project_name_in_output
        self.encountered_errors = False
        self.__info = []
        self.__warnings = []
        self.__errors = []

    def add_project_warning(self, project_name, message):
        if self.include_project_name_in_output:
            self.__warnings.append('%s - %s' % (project_name, message))
        else:
            self.__warnings.append(message)

    def add_project_error(self, project_name, message):
        self.encountered_errors = True
        if self.include_project_name_in_output:
            self.__errors.append('%s - %s' % (project_name, message))
        else:
            self.__errors.append(message)

    def add_global_info_message(self, message):
        self.__info.append(message)

    def add_global_error(self, message):
        self.encountered_errors = True
        self.__errors.append(message)

    def get_default_report(self):
        output = ''
        for line in self.__info:
            output += "[INFO]   %s\n" % line
        if len(self.__warnings) > 0:
            output += "\nFound %d Warning(s):\n" % len(self.__warnings)
            for line in self.__warnings:
                output += "[WARN]   %s\n" % line
        if len(self.__errors) > 0:
            output += "\nFound %d Error(s):\n" % len(self.__errors)
            for line in self.__errors:
                output += '[ERROR]  %s\n' % line

        return output

    def get_validation_summary(self):
        """
        :return: A concise and stable report intended for automatic testing.

        Messages are not prefixed with the project name,  INFO messages are omitted, and messages within each category
        are sorted to make the output independent of file system traversal differences.
        """
        output = ''
        for line in sorted(self.__warnings):
            output += "W:%s\n" % line
        for line in sorted(self.__errors):
            output += "E:%s\n" % line

        return output


class ProjectRulesValidator:

    def __init__(self, _project_name, _project_path, _output: OutputCollector):
        self.project_path = _project_path
        self.project_name = _project_name
        self.output = _output

    # convenience shortcut/delegate
    def add_project_warning(self, message):
        self.output.add_project_warning(self.project_name, message)

    # convenience shortcut/delegate
    def add_project_error(self, message):
        self.output.add_project_error(self.project_name, message)

    # consistency wrapper for throwing Exceptions
    def project_exception(self, message):
        return Exception(self.project_name + ' - ' + message)

    def has_project_file(self, filename):
        return os.path.isfile(os.path.join(self.project_path, filename))

    def has_project_directory(self, filename):
        return os.path.isdir(os.path.join(self.project_path, filename))

    def apply(self, enable_clean_repo_checks: bool):

        # check common/basic assumptions first

        if not self.has_project_file('.project'):
            self.add_project_error('Unexpected project layout: Missing .project file')
            return

        if not self.has_project_file('build.properties'):
            self.add_project_warning(
                'No build.properties file found; this may or may not be a violation (needs additional criteria)')
            return

        is_feature = self.has_project_file('feature.xml')
        is_bundle_or_fragment = not is_feature
        # TODO consider detecting "is fileset", too?

        has_manifest = self.has_project_file('META-INF/MANIFEST.MF')
        has_classpath = self.has_project_file('.classpath')

        # check project type/layout assumptions

        if is_feature:
            if has_manifest:
                self.add_project_error('Unexpected project layout: Feature with a manifest')
            if has_classpath:
                self.add_project_error('Unexpected project layout: Feature with a .classpath')
        else:
            if not has_manifest:
                self.add_project_error('Unexpected project layout: Bundle/fragment without a manifest')
            if not has_classpath:
                self.add_project_error('Unexpected project layout: Bundle/fragment without a .classpath')

        # common checks

        self.__check_build_properties_file()
        self.__check_project_file()

        # common checks that are only reasonable to run on a clean (pre-build, non-IDE) checkout

        if enable_clean_repo_checks:
            self.__check_extra_clean_repo_rules()

        # type-dependent checks

        if is_bundle_or_fragment:
            self.__check_classpath_file()
            self.__check_manifest_file()

        if is_feature:
            pass
            # TODO check feature.xml

        # TODO check fileset properties?

    def __check_classpath_file(self):
        matched = False
        classpath_regexp = re.compile('<classpathentry kind="output" path="([^"]+)"/>')
        with open(os.path.join(self.project_path, '.classpath'), 'r') as f:
            for line in f.readlines():
                m = classpath_regexp.search(line)
                if m:
                    if matched:
                        raise self.project_exception('Double classpath match')
                    matched = True
                    value = m.group(1)
                    if value != 'target/classes':
                        self.add_project_error(
                            "Non-standard .classpath output dir (should be 'target/classes'): %r" % value)
        if not matched:
            self.add_project_error('No class output dir line found')

    def __check_build_properties_file(self):
        output_regexp = re.compile('^output')
        with open(os.path.join(self.project_path, 'build.properties'), 'r') as f:
            for line in f.readlines():
                m = output_regexp.search(line)
                if m:
                    self.add_project_error(
                        'Found output specification in build.properties (should be absent): %r' % line)

    def __check_project_file(self):
        cs_builder_found = False
        cs_nature_found = False
        with open(os.path.join(self.project_path, 'build.properties'), 'r') as f:
            for line in f.readlines():
                if line.find('net.sf.eclipsecs.core.CheckstyleBuilder'):
                    cs_builder_found = True
                if line.find('net.sf.eclipsecs.core.CheckstyleNature'):
                    cs_nature_found = True
        if not cs_builder_found or not cs_nature_found:
            self.add_project_error('Missing or unusual Checkstyle configuration in .project file')

    def __check_manifest_package_import_clause(self, import_clause):
        parts = import_clause.split(';', maxsplit=1)
        if len(parts) == 2:
            (package, attribs) = parts
        else:
            (package, attribs) = (import_clause, None)

        if package.startswith('de.rcenvironment.'):
            if attribs:
                self.add_project_warning(
                    'Internal package import to %r should not have attributes, but declares %r' % (
                        package, attribs))
        elif package.startswith('org.osgi.'):
            pass  # no explicit rule at this time
        elif package.startswith('org.eclipse.'):
            pass  # no explicit rule at this time
        else:
            # any other import is considered "external", i.e., to a third-party library
            if not attribs:
                self.add_project_warning(
                    'External package import to %r should be versioned, but has no attributes' %
                    package)
            # TODO validate that the attributes actually are a version constraint

    def __check_manifest_file(self):
        entries = JavaManifestParser.parse_file(os.path.join(self.project_path, 'META-INF/MANIFEST.MF'),
                                                self.project_name)

        # check Import-Package: specific rules about import attributes
        import_package_value = entries.get('Import-Package', None)
        if import_package_value:
            # split this way as attributes can contain commas within quoted attribute values
            package_imports = re.split(r'(?<=[a-zA-Z"]),', import_package_value)

            for import_clause in package_imports:
                self.__check_manifest_package_import_clause(import_clause)

        # check Require-Bundle
        require_bundle_value = entries.get('Require-Bundle', None)
        if require_bundle_value:
            bundle_imports = re.split(r'(?<=[a-zA-Z"]),', require_bundle_value)

            for import_clause in bundle_imports:
                pass  # TODO

        bundle_name_value = entries.get('Bundle-Name', "")
        if not bundle_name_value.startswith('RCE '):
            self.add_project_error('Non-standard Bundle-Name: %r' % bundle_name_value)

        # check Bundle-Vendor: should be present and contain "DLR"
        if 'Bundle-Vendor' not in entries:
            self.add_project_error('Missing Bundle-Vendor in manifest')
        else:
            vendor_value = entries['Bundle-Vendor']
            if 'DLR' not in vendor_value:
                self.add_project_error('Non-standard Bundle-Vendor: %r' % vendor_value)

        # check Automatic-Module-Name: should be present
        if 'Automatic-Module-Name' not in entries:
            self.add_project_error('Missing Automatic-Module-Name in manifest')
        # TODO validate value, too?

        if 'Bundle-RequiredExecutionEnvironment' not in entries:
            self.add_project_error('Missing Bundle-RequiredExecutionEnvironment in manifest')
        else:
            exec_env_value = entries['Bundle-RequiredExecutionEnvironment']
            if exec_env_value != 'JavaSE-1.8':
                self.add_project_error('Wrong Bundle-RequiredExecutionEnvironment: %r' % exec_env_value)

    def __check_extra_clean_repo_rules(self):
        if self.has_project_directory('target'):
            self.add_project_error('Subdirectory "target" must not be present in a clean checkout')
        if self.has_project_directory('.settings'):
            self.add_project_error('Subdirectory ".settings" must not be present in a clean checkout')


def process_project(_project_name, _project_path, _enable_clean_repo_checks, _output: OutputCollector):
    if not os.path.isdir(_project_path):
        _output.add_project_error(_project_name, 'No such directory: ' + _project_path)
        return

    ProjectRulesValidator(_project_name, _project_path, _output).apply(_enable_clean_repo_checks)


def determine_list_of_projects(_output: OutputCollector, _verbose=False):
    repository_root_dir = os.path.join(os.path.dirname(__file__), RELATIVE_PATH_TO_REPOSITORY_ROOT)
    raw_subdirs = next(os.walk(repository_root_dir))[1]
    filtered_subdirs = []

    for subdir in raw_subdirs:

        if subdir.startswith('.') \
                or subdir.startswith('org.eclipse.') \
                or subdir == 'LICENSES':
            # ignore/skip these silently
            continue

        # TODO check again after reworking/renaming the launcher bundles
        if subdir == 'de.rcenvironment' \
                or subdir == 'de.rcenvironment.platform' \
                or subdir.startswith('de.rcenvironment.core.launcher'):
            _output.add_global_info_message('Skipping special project ' + subdir)
            continue

        filtered_subdirs.append(subdir)

    return filtered_subdirs


# main entry point
def execute_main():
    parser = argparse.ArgumentParser(
        description='Validates custom rules and conventions for RCE code repositories. ' +
                    'Important violations (errors) will result in a non-zero exit code for automated QA.')
    parser.add_argument('-c', '--clean-checkout', action='store_true',
                        help='Enable additional checks that are only applicable to a clean ' +
                             '(pre-build, non-IDE) repository checkout')
    args = parser.parse_args()

    output_collector = OutputCollector()

    # determine list of projects
    relative_project_dirs = determine_list_of_projects(output_collector, VERBOSE_OUTPUT)
    output_collector.add_global_info_message('Checking %d projects' % len(relative_project_dirs))

    # apply CLI parameters
    enable_clean_repo_checks = args.clean_checkout  # assign for legibility
    if enable_clean_repo_checks:
        output_collector.add_global_info_message('Additional "running on clean checkout" rules enabled')

    # scan projects
    repository_root_dir = os.path.join(os.path.dirname(__file__), RELATIVE_PATH_TO_REPOSITORY_ROOT)
    for relative_project_dir in sorted(relative_project_dirs):
        project_path = os.path.join(repository_root_dir, relative_project_dir)
        if VERBOSE_OUTPUT:
            output_collector.add_global_info_message('Checking project %s' % relative_project_dir)
        process_project(relative_project_dir, project_path, enable_clean_repo_checks, output_collector)

    # format collected output
    print(output_collector.get_default_report())

    # choose the exit code depending on whether there were errors; intended for use by CI quality gates
    if output_collector.encountered_errors:
        sys.exit(1)  # TODO decide on final exit codes; for now, only "non-zero" is sufficient
    else:
        sys.exit(0)  # TODO use a separate exit code if warnings are present, too?


if __name__ == '__main__':
    execute_main()
