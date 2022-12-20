#!/usr/bin/env python3

# Copyright 2022 DLR, Germany
#
# SPDX-License-Identifier: EPL-1.0
#
# https://rcenvironment.de/
#
# Author: Robert Mischke

import json
import os
import re
import sys

import yaml

filtering_enabled = True


def exit_with_usage():
    print('Usage: <script>.py [-a/-u] <event.log filename/location>*')
    print('       -a ("all"): print all used event ids and attributes; do not eliminate registered/known ids')
    print('       -u ("unused"): print registered event types and attributes that are not used in any event.log file')
    sys.exit(2)


def load_registered_types_and_attributes():
    global filtering_enabled

    if filtering_enabled:
        with open(os.path.join(os.path.dirname(__file__), '../resources/event-types.yml'), 'r') as f:
            event_type_data = yaml.safe_load(f)['event_types']
    else:
        event_type_data = []  # empty "loaded" data so no ids will be discarded

    # convert the registered event data into an easier layout for filtering
    result = {}
    for type_data in event_type_data:
        known_attribute_ids = set()
        for attrib_map in type_data.get('attributes', []):
            known_attribute_ids.add(attrib_map['id'])
        result[type_data['id']] = known_attribute_ids
    return result


def add_used_ids_of_event_log_line(line, collection):
    (_, type_id, attrib_string) = re.split('\\s+', line, maxsplit=2)

    if type_id not in collection:
        used_types_and_attributes[type_id] = set()

    attrib_data = json.loads(attrib_string)  # attrib_string includes the trailing newline, which is discarded here
    for (attrib_key, _) in attrib_data.items():
        if attrib_key == '':
            continue  # for now, ignore any encountered empty-key attributes
        collection[type_id].add(attrib_key)


def subtract_id_sets(original_set, set_to_subtract):
    result = {}  # dict(event id -> set(attribute ids))

    for (type_id, attribute_id_set) in original_set.items():

        if type_id not in set_to_subtract:
            result[type_id] = attribute_id_set
            continue

        attribute_id_set_to_substract = set_to_subtract[type_id]
        entry_created = False
        for attrib_id in attribute_id_set:
            if attrib_id not in attribute_id_set_to_substract:
                if not entry_created:
                    result[type_id] = set()
                    entry_created = True
                result[type_id].add(attrib_id)

    return result


def print_result_as_yaml_template(output_data):
    for (event_type, attrib_keys) in output_data.items():
        print("- id: %s" % event_type)
        print("  desc: TODO")
        if len(attrib_keys) > 0:
            print("  attributes:")
            for attrib_key in attrib_keys:
                print("  - id: %s" % attrib_key)
                print("    desc: TODO")


# parse parameters

args = sys.argv[1:]
while len(args) > 0 and args[0].startswith('-'):
    if args[0] == '-a':
        filtering_enabled = False
    else:
        print('Invalid flag: ' + args[0])
        exit_with_usage()
    args = args[1:]

# load registered events and attribute ids (unless filtering is disabled)

registered_types_and_attributes = load_registered_types_and_attributes()  # dict(event id -> set(attribute ids))

# extract all event type ids and attribute ids from the provided event.log files

used_types_and_attributes = {}  # dict(event id -> set(attribute ids))
for event_log_filename in args:
    with open(event_log_filename, 'r') as f:
        for file_line in f:
            add_used_ids_of_event_log_line(file_line, used_types_and_attributes)

# build the id set difference as/if requested

if filtering_enabled:
    """
    if filtering_inverted:
        # TODO actually invert
        final_result = subtract_id_sets(registered_types_and_attributes, used_types_and_attributes)
    else:
        final_result = subtract_id_sets(used_types_and_attributes, registered_types_and_attributes)
    """
    print("Registered types and attributes that are not used in any provided event log file:\n")
    print_result_as_yaml_template(subtract_id_sets(registered_types_and_attributes, used_types_and_attributes))

    print("\nUnregistered types and attributes used in at least one provided event log file:\n")
    print_result_as_yaml_template(subtract_id_sets(used_types_and_attributes, registered_types_and_attributes))
else:
    print("All types and attributes used in at least one provided event log file:\n")

    print_result_as_yaml_template(used_types_and_attributes)
