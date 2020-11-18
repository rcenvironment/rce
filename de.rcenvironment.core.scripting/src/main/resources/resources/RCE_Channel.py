'''
Created on 24.04.2013

@author: Sascha Zur
@author: Jascha Riedel (#14029)
@author: Adrian Stock
@author: Kathrin Schaffert (#17088)
'''
import simplejson as json
import math
import os
from decimal import *
from io import IOBase 

import input_file_factory as IFF

def create_input_file():
	return IFF.InputFileFactory(json.OrderedDict(),RCE_INPUT_FILES)

def readinput_internal():
    """ 
    INTERNAL METHOD
    Reads the input channel values from RCE
    """
    try:
        return json.load(open("pythonInput.rced", "r"))
    except IOError:
        return {}

def readinput_req_if_connected_internal():
    """ 
    INTERNAL METHOD
    Reads the input channel that are only required if connected
    """
    try:
        return json.load(open("pythonInputReqIfConnected.rced", "r"))
    except IOError:
        return []
    
def read_state_variables_internal():
    """ 
    INTERNAL METHOD
    Reads the state variable values from RCE
    """
    try:
        return json.load(open("pythonStateVariables.rces", "r"))
    except IOError:
        return {}

def read_output_names_internal():
    """
    INTERNAL METHOD
    Reads the names of all output channel from RCE. This is for writing the correct channel back to RCE.
    """
    try:
        return json.load(open("outputs.rceo", "r"))
    except IOError:
        return {}
    
def read_run_number_internal():
    """
    INTERNAL METHOD
    Reads the names of all output channel from RCE. This is for writing the correct channel back to RCE.
    """
    try:
        return json.load(open("pythonRunNumber.rcen", "r"))
    except IOError:
        return -1

def writeoutput_internal():
    """ 
    INTERNAL METHOD
    Writes the output to RCE
    """
    outputfile = open("pythonOutput.rced", "w")
    json.dump(RCE_CHANNEL_OUTPUT, outputfile)
    closeoutputsfile = open("pythonCloseOutputChannelsList.rced", "w")
    json.dump(RCE_CHANNEL_CLOSE, closeoutputsfile)
    statevariablesfile = open("pythonStateOutput.rces", "w")
    json.dump(RCE_STATE_VARIABLES, statevariablesfile)
    writteninputfiles = open("pythonInputFileFactoryOutput.rced", "w")
    json.dump(RCE_INPUT_FILES, writteninputfiles)
    
   
def read_input(name, defaultValue = None):
    """ 
    Gets the value for the given input name or returns the default value if there is no input connected and the input not required
    """
    if name in RCE_CHANNEL_REQ_IF_CONNECTED and (defaultValue is None):
        raise ValueError("Input " + str(name) + " not connected.")
    elif name in RCE_CHANNEL_REQ_IF_CONNECTED and not (defaultValue is None):
        return defaultValue
    else:
        if name in RCE_CHANNEL_INPUT.keys():
            return RCE_CHANNEL_INPUT[name]
        else:
            raise ValueError("Input '" + str(name) + "' is not defined or it has no value")
            return None
    
def close_output(name):
    """ 
    Closes the RCE channel of the given output
    """
    if not (name in RCE_CHANNEL_OUTPUT_NAMES):
        raise NameError("Output '" + name + "' is not defined")
    if not name in RCE_CHANNEL_CLOSE:
        RCE_CHANNEL_CLOSE.append(name)

def write_output(name, value):
    """ 
    Sets the given value to the output name which will be read from RCE
    """
    if not (name in RCE_CHANNEL_OUTPUT_NAMES):
        raise ValueError("Output '" + name + "' is not defined")
    if not name in RCE_CHANNEL_OUTPUT:
        RCE_CHANNEL_OUTPUT[name] = []
    if isinstance(value, list):
        for index, elem in enumerate(value):
             if isinstance(elem, list):
                 for index2, elem2 in enumerate(elem):
                     if (__isInf__(elem2)):
                         elem[index2] = "+Infinity"
             else:
                 if (__isInf__(elem)):
                     value[index] = "+Infinity"
    if (__isInf__(value)):
        RCE_CHANNEL_OUTPUT[name].append("+Infinity")
    else:
        RCE_CHANNEL_OUTPUT[name].append(value)

def __isInf__(value):
    infinity = ((type(value) is float) and float(value) > 0 and not (float(value) < float('inf')));
    if (type(value) is Decimal and value  == Decimal('Infinity')):
        infinity = True;
    return infinity;

def write_not_a_value_output(name):
    if not (name in RCE_CHANNEL_OUTPUT_NAMES):
        raise ValueError("Output '" + name + "' is not defined")
    if not (name in RCE_CHANNEL_OUTPUT):
        RCE_CHANNEL_OUTPUT[name] = []
    RCE_CHANNEL_OUTPUT[name].append("not_a_value_7fdc603e")

def getallinputs():
    """
    Gets the dictionary with all inputs from RCE
    """
    joined = []
    for item in dict(RCE_CHANNEL_INPUT).keys():
        joined.append(item)
    for item in RCE_CHANNEL_REQ_IF_CONNECTED:
        joined.append(item)
    return joined
def get_input_names_with_datum():
    return dict(RCE_CHANNEL_INPUT).keys()

def get_output_names():
    """ 
    Returns the read names of all output channel from RCE
    """
    return RCE_CHANNEL_OUTPUT_NAMES

def write_state_variable(name, value):
    """ 
    Writes a variable name in the dictionary for the components state
    """
    RCE_STATE_VARIABLES[name] = value

def read_state_variable(name):
    """ 
    Reads the given state variables value, if it exists, else an error is raised
    """
    if not str(name) in RCE_STATE_VARIABLES:
        raise ValueError("No state variable stored for '" + str(name) + "'")
    else :
        return RCE_STATE_VARIABLES[name]
    
def read_state_variable(name, defaultValue = None):
    """ 
    Reads the given state variables value, if it exists, else a default value is returned and stored in the dict
    """
    if str(name) in RCE_STATE_VARIABLES:
       return RCE_STATE_VARIABLES[name]
    else:
       if (defaultValue is None):
         return defaultValue
       else:
         RCE_STATE_VARIABLES[name] = defaultValue    
         return defaultValue
    
def read_state_variable_default(name, defaultValue):
    print("The method 'read_state_variable_default' is deprecated. Please use 'read_state_variable(name, defaultValue).")
    return read_state_variable(name, defaultValue)
    
def get_state_dict():
    """ 
    Returns the current state dictionary
    """
    if RCE_STATE_VARIABLES:
        return RCE_STATE_VARIABLES
    else:
        return {} 

def get_execution_count():
    """
    Returns the current run number of the RCE component
    """
    return RCE_CURRENT_RUN_NUMBER
    
def fail(reason):
    """
    Fails the RCE component with the given reason
    """
    raise Exception(reason)

def close_all_outputs():
    for output in RCE_CHANNEL_OUTPUT_NAMES:
        close_output(output)

def init_module():
    global RCE_CHANNEL_INPUT, RCE_CHANNEL_REQ_IF_CONNECTED, RCE_CHANNEL_OUTPUT_NAMES, RCE_CHANNEL_OUTPUT, RCE_CHANNEL_CLOSE, RCE_STATE_VARIABLES, RCE_CURRENT_RUN_NUMBER
    RCE_CHANNEL_INPUT = readinput_internal()
    RCE_CHANNEL_REQ_IF_CONNECTED = readinput_req_if_connected_internal()
    RCE_CHANNEL_OUTPUT_NAMES = read_output_names_internal()
    RCE_CHANNEL_OUTPUT = {}
    RCE_CHANNEL_CLOSE = []
    RCE_STATE_VARIABLES = read_state_variables_internal()
    RCE_CURRENT_RUN_NUMBER = read_run_number_internal()
    
def show_variables():
	print("RCE_CHANNEL_INPUT:",RCE_CHANNEL_INPUT)
	print("RCE_CHANNEL_REQ_IF_CONNECTED:",RCE_CHANNEL_REQ_IF_CONNECTED)
	print("RCE_CHANNEL_OUTPUT_NAMES:",RCE_CHANNEL_OUTPUT_NAMES)
	print("RCE_CHANNEL_OUTPUT:",RCE_CHANNEL_OUTPUT)
	print("RCE_CHANNEL_CLOSE:",RCE_CHANNEL_CLOSE)
	print("RCE_STATE_VARIABLES:",RCE_STATE_VARIABLES)
	print("RCE_CURRENT_RUN_NUMBER:",RCE_CURRENT_RUN_NUMBER)

RCE_CHANNEL_INPUT = readinput_internal()
RCE_CHANNEL_REQ_IF_CONNECTED = readinput_req_if_connected_internal()
RCE_CHANNEL_OUTPUT_NAMES = read_output_names_internal()
RCE_CHANNEL_OUTPUT = {}
RCE_CHANNEL_CLOSE = []
RCE_STATE_VARIABLES = read_state_variables_internal()
RCE_CURRENT_RUN_NUMBER = read_run_number_internal()
RCE_INPUT_FILES = []