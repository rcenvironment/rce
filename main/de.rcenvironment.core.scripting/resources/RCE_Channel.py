'''
Created on 24.04.2013

@author: Sascha Zur
'''
import simplejson as json

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
    indefiniteOutputs = open("pythonSetOutputsIndefinit.rceo", "w")
    json.dump(RCE_INDEFINITE_OUTPUTS, indefiniteOutputs)
    
   
def read_input(name, defaultValue = None):
    """ 
    Gets the value for the given input name or returns the default value if there is no input connected and the input not required
    """
    if name in RCE_CHANNEL_REQ_IF_CONNECTED and (defaultValue is None):
        raise ValueError("Input " + str(name) + " not connected.")
    elif name in RCE_CHANNEL_REQ_IF_CONNECTED and not (defaultValue is None):
        return defaultValue
    else:
        if RCE_CHANNEL_INPUT.has_key(name):
            return RCE_CHANNEL_INPUT[name]
        else:
            raise ValueError("Input " + str(name) + " not defined or has no value.")
            return None
    
def close_output(name):
    """ 
    Closes the RCE channel of the given output
    """
    if not (name in RCE_CHANNEL_OUTPUT_NAMES):
        raise NameError('Output ' + name + ' does not exist.')
    if not name in RCE_CHANNEL_CLOSE:
        RCE_CHANNEL_CLOSE.append(name)

def write_output(name, value):
    """ 
    Sets the given value to the output name which will be read from RCE
    """
    if not (name in RCE_CHANNEL_OUTPUT_NAMES):
        raise ValueError('Output ' + name + ' not defined.')
    if not name in RCE_CHANNEL_OUTPUT:
        RCE_CHANNEL_OUTPUT[name] = []
    RCE_CHANNEL_OUTPUT[name].append(value)

def write_not_a_value_output(name):
    """ 
    Sets the given output to indefinite data type
    """
    if not (name in RCE_CHANNEL_OUTPUT_NAMES):
        raise NameError('Output ' + name + ' does not exist.')
    if not name in RCE_INDEFINITE_OUTPUTS:
        RCE_INDEFINITE_OUTPUTS.append(name)

def getallinputs():
    """
    Gets the dictionary with all inputs from RCE
    """
    return dict(RCE_CHANNEL_INPUT).keys()

def get_input_names_with_datum():
    return getallinputs()

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
        raise ValueError("No value for " + str(name) + " defined!")
    else :
        return RCE_STATE_VARIABLES[name]
    
def read_state_variable(name, defaultValue = 0):
    """ 
    Reads the given state variables value, if it exists, else a default value is returned and stored in the dict
    """
    if str(name) in RCE_STATE_VARIABLES:
       return RCE_STATE_VARIABLES[name]
    else:
        RCE_STATE_VARIABLES[name] = defaultValue
        return defaultValue 
    
def read_state_variable_default(name, defaultValue):
    print "The method 'read_state_variable_default' is deprecated. Please use 'read_state_variable(name, defaultValue)."
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

RCE_CHANNEL_INPUT = readinput_internal()
RCE_CHANNEL_REQ_IF_CONNECTED = readinput_req_if_connected_internal()
RCE_CHANNEL_OUTPUT_NAMES = read_output_names_internal()
RCE_CHANNEL_OUTPUT = {}
RCE_CHANNEL_CLOSE = []
RCE_INDEFINITE_OUTPUTS = []
RCE_STATE_VARIABLES = read_state_variables_internal()
RCE_CURRENT_RUN_NUMBER = read_run_number_internal()