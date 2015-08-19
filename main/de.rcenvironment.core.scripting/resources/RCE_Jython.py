class RCE:
	def setDictionary_internal(self,value):
		"""
    	INTERNAL METHOD
    	"""
		global dictOut
		global dictIn 
		global listClose
		global listIndef
		dictIn = value
		dictOut = {}
		listClose = []
		listIndef = []
	def get_closed_outputs_internal(self):
		"""
    	INTERNAL METHOD
    	"""
		return listClose
	def get_indefinite_outputs_internal(self):
		"""
    	INTERNAL METHOD
    	"""
		return listIndef
	
	def get_output_internal(self):
		"""
    	INTERNAL METHOD
    	"""
		return dictOut
		
		
	def read_input(self, name, defaultValue = None):
   		 """ 
  		 Gets the value for the given input name or returns the default value if there is no input connected and the input not required
  		 """
   		 if name in RCE_LIST_REQ_IF_CONNECTED_INPUTS and (defaultValue is None):
   		 	raise ValueError("Input " + str(name) + " not connected.")
   		 elif name in RCE_LIST_REQ_IF_CONNECTED_INPUTS and not (defaultValue is None):
   		 	return defaultValue
		 else:
		 	 if name not in dictIn:
			 	raise ValueError("Input " + str(name) + " not defined or has no value.")
			 else: 
				return dictIn[name]

	def write_output(self,key,value):
		if not str(key) in RCE_LIST_OUTPUTNAMES:
			raise ValueError("Output " + str(key) + " not defined.")
		else :
			if key in dictOut:
				dictOut[key].append(value)
			else:
				list = [value]
				dictOut.update({key:list})

	def write_not_a_value_output(self, name):
		if not str(name) in RCE_LIST_OUTPUTNAMES:
			raise NameError("Output " + str(name) + " does not exist.")
		if not name in listIndef:
			listIndef.append(name)
			
	def close_output(self,key):
		if not str(key) in RCE_LIST_OUTPUTNAMES:
			raise NameError("Output " + str(key) + " does not exist.")
		if not key in listClose:
			listClose.append(key)

	def write_state_variable(self, name, value):
		""" 
		Writes a variable name in the dictionary for the components state
		"""
		RCE_STATE_VARIABLES[name] = value
	
	def read_state_variable(self, name):
		""" 
		Writes a variable name in the dictionary for the components state
		"""		
		if not str(name) in RCE_STATE_VARIABLES:
			raise ValueError("No value for " + str(name) + " defined!")
		else :
			return RCE_STATE_VARIABLES[name]
	def read_state_variable(self, name, defaultValue = 0):
		""" 
		Reads the given state variables value, if it exists, else an error is raised
		"""
		if str(name) in RCE_STATE_VARIABLES:
			return RCE_STATE_VARIABLES[name]
		else:
			RCE_STATE_VARIABLES[name] = defaultValue
			return defaultValue
		
	def read_state_variable_default(self, name, defaultValue):
		print "The method 'read_state_variable_default' is deprecated. Please use 'read_state_variable(name, defaultValue)."
		return RCE.read_state_variable(name, defaultValue)
 
	def get_state_dict(self):
		"""
		Returns the current state dictionary.
		"""
		if RCE_STATE_VARIABLES:
			return RCE_STATE_VARIABLES
		else:
			return {}
		
	def get_input_names_with_datum(self):
		return dictIn.keys()
    
   	def get_execution_count(self):
   		return RCE_CURRENT_RUN_NUMBER
   	
   	def fail(self, reason):
   		raise Exception(reason)
   	
   	def close_all_outputs(self):
   		for output in RCE_LIST_OUTPUTNAMES:
   			RCE.close_output(output)
   	
   	def get_output_names(self):
   		return RCE_LIST_OUTPUTNAMES
   	
# set RCE object.
RCE = RCE()
import sys

# if there is no valid path to the jython.jar in sys.path
# then set it!
RCE_Boolean_Path_Controller = False
for line in sys.path:
	if line == RCE_Bundle_Jython_Path:
		RCE_Boolean_Path_Controller = True

if RCE_Boolean_Path_Controller == False:
	sys.path.append(RCE_Bundle_Jython_Path)
import os
if not os.path.exists(RCE_Temp_working_path):
	os.mkdir(RCE_Temp_working_path)
os.chdir(RCE_Temp_working_path)
