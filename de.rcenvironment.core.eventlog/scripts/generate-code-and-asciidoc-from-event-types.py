#!/usr/bin/env python3

# Copyright 2022 DLR, Germany
#
# SPDX-License-Identifier: EPL-1.0
#
# https://rcenvironment.de/
#
# Author: Robert Mischke

# Note: We usually avoid non-standard dependencies in RCE "utility" scripts to simplify their usage. But in this case,
# the benefits of using a proper templating engine far outweigh the downsides.  -- misc_ro
import jinja2
import os
import re
import yaml
from collections import OrderedDict

# path and code generation constants

YML_SPEC_FILE_PATH = '../resources/event-types.yml'

JAVA_CLASS_NAME = 'EventType'

JAVA_PACKAGE = 'de.rcenvironment.core.eventlog.api'

# NOTE: not placed in /src/generated/java (or similar) to avoid handling overhead
GENERATED_JAVA_FILE_PATH = '../src/main/java/%s/%s.java' % (JAVA_PACKAGE.replace('.', '/'), JAVA_CLASS_NAME)

GENERATED_ASCIIDOC_FILE_PATH = '../../de.rcenvironment.documentation.core/src/asciidoc/generated/event-types.ad'


class ProcessedEventData:
    ordered_data = []

    attribute_data = None
    MAXIMUM_ATTRIBUTE_TITLE_LENGTH = 0

    def __init__(self, _raw_yaml_data):

        self.MAXIMUM_ATTRIBUTE_ID_LENGTH = _raw_yaml_data['properties']['max_attribute_id_length']
        self.MAXIMUM_ATTRIBUTE_TITLE_LENGTH = _raw_yaml_data['properties']['max_attribute_title_length']
        # meta-validation :)
        if self.MAXIMUM_ATTRIBUTE_ID_LENGTH < 3 or self.MAXIMUM_ATTRIBUTE_ID_LENGTH > 50:
            raise Exception('Implausible maximum attribute id length')
        if self.MAXIMUM_ATTRIBUTE_TITLE_LENGTH < 3 or self.MAXIMUM_ATTRIBUTE_TITLE_LENGTH > 80:
            raise Exception('Implausible maximum attribute title length')

        raw_event_data = _raw_yaml_data['event_types']

        # build id-based, two-level lookup dictionary for resolving cross-references
        cross_reference_dict = {}
        for entry in raw_event_data:
            attribute_dict = {}
            if 'attributes' in entry:
                for attrib_entry in entry['attributes']:
                    attribute_dict[attrib_entry['id']] = attrib_entry
            entry['attribute_lookup'] = attribute_dict
            cross_reference_dict[entry['id']] = entry

        for entry in raw_event_data:
            # resolve cross-references
            self.__resolve_cross_references(cross_reference_dict, entry)
            self.__normalize_and_transform_event_type_data(entry)
            self.ordered_data.append(entry)  # maintain original order

        # validate attributes, esp. regarding titles
        for entry in raw_event_data:
            if 'attributes' in entry:
                for attrib_entry in entry['attributes']:
                    self.__validate_attribute_entry(attrib_entry, entry)

        self.attribute_data = self.__extract_attributes(self.ordered_data)  # use data with resolved cross-references

    def __normalize_and_transform_event_type_data(self, entry):
        # normalize description: remove surrounding whitespace, always finish with a full stop (Javadoc convention)
        temp = entry['desc'].strip()
        if not temp.endswith('.'):
            temp = temp + '.'
        entry['desc'] = temp

    def __validate_attribute_entry(self, attrib_entry, entry):
        entry_id = entry['id']
        attrib_entry_id = attrib_entry['id']
        if len(attrib_entry_id) > self.MAXIMUM_ATTRIBUTE_ID_LENGTH:
            print('Id of attribute %s:%s has problematic length %d' % (
                entry_id, attrib_entry_id, len(attrib_entry_id)))
        if 'title' not in attrib_entry:
            print('Attribute %s:%s should have a title' % (entry_id, attrib_entry_id))
            return
        title = attrib_entry['title']
        if title.strip() != title:
            print('Title of attribute %s:%s has surrounding whitespace' % (entry_id, attrib_entry_id))
        if len(title) < 3 or len(title) > self.MAXIMUM_ATTRIBUTE_TITLE_LENGTH:
            print('Title of attribute %s:%s has problematic length %d: %r' % (
                entry_id, attrib_entry_id, len(title), title))

    # note: modifies the provided entry
    # note: no explicit error checking by now; reference errors will throw exceptions
    def __resolve_cross_references(self, cross_reference_dict, entry):

        # process top-level fields
        for attrib_property_id in ['desc']:
            if entry[attrib_property_id].startswith('copy-from'):
                ref_id = self.__parse_cross_reference(entry[attrib_property_id])
                entry[attrib_property_id] = cross_reference_dict[ref_id][attrib_property_id]

        # process attributes' fields
        # TODO could use some refactoring for clarity
        for attribute_entry in entry.get('attributes', []):
            for attrib_property_id in ['desc', 'title', 'values']:
                attrib_field_value = attribute_entry.get(attrib_property_id, None)
                if isinstance(attrib_field_value, str) and attrib_field_value.startswith('copy-from'):
                    ref_id = self.__parse_cross_reference(attrib_field_value)
                    try:
                        # overwrite
                        attribute_entry[attrib_property_id] = \
                            cross_reference_dict[ref_id]['attribute_lookup'][attribute_entry['id']][
                                attrib_property_id]
                    except KeyError as e:
                        raise Exception(
                            'Failed to resolve cross-reference %r in event %r, attribute %r, property %r' % (
                                attrib_field_value, entry['id'], attribute_entry['id'], attrib_property_id), e)

    @staticmethod
    def __parse_cross_reference(_input):
        m = re.match('copy-from:\\(([a-z.]+)\\)', _input)
        if not m:
            raise Exception('Malformed cross-reference: ' + _input)
        return m.group(1)

    @staticmethod
    def __extract_attributes(_ordered_data):
        result = OrderedDict()
        for entry in _ordered_data:
            for attribute_entry in entry.get('attributes', []):
                attrib_key = attribute_entry['id']
                if attrib_key not in result:
                    result[attrib_key] = {
                        'descriptions': set(),  # not used yet
                        'used_by': set()
                    }
                result[attrib_key]['descriptions'].add(attribute_entry['desc'])
                result[attrib_key]['used_by'].add(entry['id'])

        return result


def render_event_types_to_java_class(event_type_data, attribute_data, output):
    # Template formatting note: The template aims to match the Eclipse formatter rules for RCE. The goal is that
    # reformatting the generated file should not cause any difference. This should be tested after any modification.
    #
    # These formatter rules imply word wrapping to 140 characters, so any word wrapping filters are parameterized to
    # the length of (140 - line prefix length) characters.

    # event data pre-processing to avoid cluttering the template
    for type_entry in event_type_data:  # maintain manual order as in YAML file

        # define the constant name in one place
        type_entry['constant_name'] = type_entry['id'].upper().replace('.', '_')

        for attrib_entry in type_entry.get('attributes', []):

            # assemble per-attribute JavaDoc contribution
            attrib_id = attrib_entry['id']
            attrib_desc = attrib_entry['desc']
            suffix = ''
            # note: these attributes are encoded as 'yes'/'no' in YAML, but loaded as booleans
            if attrib_entry.get('optional', False):
                suffix += ' <i>(optional)</i>'
            if attrib_entry.get('derived', False):
                suffix += ' <i>(derived)</i>'
            attrib_entry['event_javadoc_contrib'] = '<li><b>%s</b>%s: %s' % (attrib_id, suffix, attrib_desc)

            # define the constant name in one place
            attrib_entry['constant_name'] = attrib_id.upper()

    # similar pre-processing for the aggregated attribute data
    for (attrib_key, attrib_metadata) in attribute_data.items():
        # assemble "used by" JavaDoc for each entry in "Attributes" class
        user_type_constants = [_id.upper().replace('.', '_') for _id in attrib_metadata['used_by']]
        line_content = 'Used by %s.' % ', '.join(sorted(user_type_constants))
        attrib_metadata['used_by_string'] = line_content

    j2t = jinja2.Template("""\
/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package {{ JAVA_PACKAGE }};

import java.util.Optional;

/**
 * Provides common/default type id constants for event log entries. Extensions or plugins may provide and use additional ids, but all code
 * that is part of a standard RCE distribution should place their ids here.
 *
 * @author Robert Mischke (template)
 */
public enum {{ JAVA_CLASS_NAME }} {

    // THIS IS A GENERATED FILE - ANY MANUAL CHANGES WILL BE OVERWRITTEN!

    // For modifications, edit '{{ SPEC_FILENAME }}' and rerun 'scripts/{{ SCRIPT_FILENAME }}'.

    {% for event in event_data -%}
    /**
     * {{ event.desc | wordwrap(133, wrapstring='
     * ') }}
     {%- if event.attributes %}
     *
     * <p>
     * Attributes:
     * <ul>
     {%- for attrib in event.attributes %}
     * {{ attrib.event_javadoc_contrib | wordwrap(133, wrapstring='
     * ') }}
     {%- endfor %}
     * </ul>
     {%- endif %}
     */
    {% macro enum_declaration_start(event) -%}
    {# Represent the enum declaration as a macro so the resulting output can be word wrapped. #}
    {{- event.constant_name }}("{{ event.id }}", "{{ event.title }}", new String[] { {% for attrib in event.attributes -%}
    Attributes.{{ attrib.constant_name }}{% if not loop.last %}, {% endif %} 
    {%- endfor %}
    {%- endmacro -%}
    {{ enum_declaration_start(event) | wordwrap(133, wrapstring='
        ') }}
    }, new String[] {
    {%- for attrib in event.attributes %}
        "{{ attrib.title }}"{% if not loop.last %},{% endif %} 
    {%- endfor %}
    })
    {%- if loop.last %};{% else %},{% endif %}

    {% endfor -%}
    /**
     * The attribute keys used by the event types above. Except for the special event type {@link #CUSTOM}, which may use custom attribute
     * keys in addition to the required "type" attribute, this list comprises all valid key values for attributes.
     */
    public static class Attributes {
        {%- for attrib_key, attrib_metadata in attribute_data %}

        /**
         * {{ attrib_metadata.used_by_string | wordwrap(129, wrapstring='
         * ') }}
         */
        public static final String {{ attrib_key | upper }} = "{{ attrib_key }}";
        {%- endfor %}

    }

    private final String id;

    private final String title;

    private String[] attributeKeys;

    private String[] attributeTitles;

    EventType(String typeId, String title, String[] attributeKeys, String[] attributeTitles) {
        this.id = typeId;
        this.title = title;
        this.attributeKeys = attributeKeys;
        this.attributeTitles = attributeTitles;
        if (attributeKeys.length != attributeTitles.length) {
            throw new IllegalArgumentException();
        }
    }

    public String getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public String[] getAttributeKeys() {
        // note: the returned array is mutable; clients modifying it is considered a coding error
        return attributeKeys;
    }

    public Optional<String> getAttributeTitle(String key) {
        for (int i = 0; i < attributeKeys.length; i++) {
            if (key.equals(attributeKeys[i])) {
                return Optional.of(attributeTitles[i]);
            }
        }
        return Optional.empty();
    }

}

""")

    output.write(j2t.render(
        # more or less constant values in uppercase
        SPEC_FILENAME=YML_SPEC_FILE_PATH.replace('../', ''),
        SCRIPT_FILENAME=os.path.basename(__file__),
        JAVA_PACKAGE=JAVA_PACKAGE,
        JAVA_CLASS_NAME=JAVA_CLASS_NAME,
        # loaded data in lowercase
        attribute_data=attribute_data.items(),
        event_data=event_type_data
    ))


# TODO convert to Jinja template?
def render_event_types_to_asciidoc(event_type_data, output):
    output.write('[options = "header"]' + "\n")
    output.write('|=======================' + "\n")
    output.write('|Event Type (Title / Id / Description) |Attribute (Title / Id) |Attribute Description' + "\n")

    for type_entry in event_type_data:  # TODO sort alphabetically or keep file order?

        # normalize attribute array, and ensure there is at least one entry
        attributes = type_entry.get('attributes', [])
        if len(attributes) == 0:
            attributes = [{'id': '- __(no attributes)__'}]
        attribute_count = len(attributes)

        attrib_entry = attributes[0]
        output.write(".%d+|%s |%s |%s\n" %
                     (attribute_count,
                      __render_asciidoc_type_entry_id_and_desc(type_entry),
                      __render_asciidoc_attribute_id(attrib_entry),
                      __render_asciidoc_attribute_description(attrib_entry)))
        for attrib_id in range(2, attribute_count + 1):
            attrib_entry = attributes[attrib_id - 1]

            attrib_id_text = __render_asciidoc_attribute_id(attrib_entry)

            attrib_desc_text = __render_asciidoc_attribute_description(attrib_entry)

            output.write("|%s |%s\n" % (attrib_id_text, attrib_desc_text))

    output.write('|=======================' + "\n")


def __render_asciidoc_type_entry_id_and_desc(type_entry):
    if 'title' in type_entry:
        title = type_entry['title']
    else:
        title = type_entry['id']
    return '*%s* +\n[small]#__(%s)__#\n\n%s' % (title, type_entry['id'], type_entry['desc'])


def __render_asciidoc_attribute_id(attrib_entry):
    if 'id' not in attrib_entry:
        return '[DATA ERROR]'
    if 'title' in attrib_entry:
        return "%s{nbsp} [small]#__(%s)__#" % (attrib_entry['title'], attrib_entry['id'])
    else:
        return attrib_entry['id']


def __render_asciidoc_attribute_description(attrib_entry):
    attrib_desc_text = attrib_entry.get('desc', '')
    attrib_values = attrib_entry.get('values', [])
    if len(attrib_values) > 0:
        attrib_desc_text += " +\nPossible values: __" + '__, __'.join(attrib_values) + '__'
    flags_string = None
    if 'optional' in attrib_entry:
        if not attrib_entry['optional']:
            raise Exception('Attribute "optional" field should be either a YAML "true" value or absent')
        flags_string = 'optional'
    if 'derived' in attrib_entry:
        if not attrib_entry['derived']:
            raise Exception('Attribute "derived" field should be either a YAML "true" value or absent')
        if flags_string:
            flags_string += ', derived'
        else:
            flags_string = 'derived'
    if flags_string:
        attrib_desc_text += " +\n[small]#__(%s)__#" % flags_string
    return attrib_desc_text


script_dir = os.path.dirname(__file__)

# load and parse data
raw_data = yaml.safe_load(open(os.path.join(script_dir, YML_SPEC_FILE_PATH), 'r'))
data = ProcessedEventData(raw_data)

# generate Java code
with open(os.path.join(script_dir, GENERATED_JAVA_FILE_PATH), 'w+') as f:
    render_event_types_to_java_class(data.ordered_data, data.attribute_data, f)

# generate AsciiDoc
with open(os.path.join(script_dir, GENERATED_ASCIIDOC_FILE_PATH), 'w+') as f:
    render_event_types_to_asciidoc(data.ordered_data, f)
