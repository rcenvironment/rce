class RCE:
	def setDictionary_internal(self,value):
		"""
    	INTERNAL METHOD
    	"""
		global dictOut
		global dictIn 
		global listClose
		dictIn = value
		dictOut = {}
		listClose = []
	def get_closed_outputs_internal(self):
		"""
    	INTERNAL METHOD
    	"""
		return listClose
	
	def get_output_internal(self):
		"""
    	INTERNAL METHOD
    	"""
		return dictOut
	
	def getallinputs(self):
		"""
    	Gets the dictionary with all inputs from RCE
    	"""
	   	return dictIn.keys() + RCE_LIST_REQ_IF_CONNECTED_INPUTS
	
	def read_input(self, name, defaultValue = None):
   		 """ 
  		 Gets the value for the given input name or returns the default value if there is no input connected and the input not required
  		 """
   		 if name in RCE_LIST_REQ_IF_CONNECTED_INPUTS and (defaultValue is None):
   		 	raise ValueError("Input '" + str(name) + "' is not connected")
   		 elif name in RCE_LIST_REQ_IF_CONNECTED_INPUTS and not (defaultValue is None):
   		 	return defaultValue
		 else:
		 	 if name not in dictIn:
		 	 	if (defaultValue is None):
			 		raise ValueError("Input '" + str(name) + "' is not defined or it has no value")
			 	else:
			 		return defaultValue
			 else: 
				return dictIn[name]

	def write_output(self,key,value):
		if not str(key) in RCE_LIST_OUTPUTNAMES:
			raise ValueError("Output '" + str(key) + "' is not defined")
		elif(isinstance(value, complex)):
			raise ValueError("Value '" + str(value) + "' for Output '" + key + "' is complex, which is not supported by RCE")
		else :
			from decimal import Decimal
			if (type(value) is Decimal and value  == Decimal('Infinity')):
				dictOut[key].append(float("Infinity"))
			elif (type(value) is Decimal and value  == Decimal('-Infinity')):
				dictOut[key].append(float("-Infinity"))
			elif (type(value) is Decimal and Decimal.is_nan(value)):
				dictOut[key].append(float("nan"))
			elif isinstance(value, list):
				for index, elem in enumerate(value):
					if isinstance(elem, list):
						for index2, elem2 in enumerate(elem):
							if (type(elem2) is Decimal and elem2  == Decimal('Infinity')):
								elem[index2] = "+Infinity";
							elif(type(elem2) is Decimal and elem2  == Decimal('-Infinity')):
								elem[index2] = "-Infinity";
							elif(type(elem2) is Decimal and Decimal.is_nan(elem2)):
								elem[index2] = float("nan");
							else:
								if (type(elem) is Decimal and elem  == Decimal('Infinity')):
									value[index] = "+Infinity";
								if (type(elem) is Decimal and elem  == Decimal('-Infinity')):
									value[index] = "-Infinity";
								if (type(elem) is Decimal and Decimal.is_nan(elem)):
									value[index] = float ("nan"); 
				if key in dictOut:
					dictOut[key].append(value)
				else:
					newlist = [value]
					dictOut.update({key:newlist})
			elif key in dictOut:
				dictOut[key].append(value)
			else:
				newlist = [value]
				dictOut.update({key:newlist})

	def write_not_a_value_output(self, name):
		if not str(name) in RCE_LIST_OUTPUTNAMES:
			raise NameError("Output '" + str(name) + "' is not defined")
		else :
			if name in dictOut:
				dictOut[name].append("not_a_value_7fdc603e")
			else:
				list = ["not_a_value_7fdc603e"]
				dictOut.update({name:list})

			
	def close_output(self,key):
		if not str(key) in RCE_LIST_OUTPUTNAMES:
			raise NameError("Output '" + str(key) + "' is not defined")
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
			raise ValueError("No state variable stored for '" + str(name) + "'")
		else :
			return RCE_STATE_VARIABLES[name]
	def read_state_variable(self, name, defaultValue = None):
		""" 
		Reads the given state variables value, if it exists, else an error is raised
		"""
		if str(name) in RCE_STATE_VARIABLES:
			return RCE_STATE_VARIABLES[name]
		else:
			if (defaultValue is None):
				return defaultValue
			else:
				RCE_STATE_VARIABLES[name] = defaultValue	
				return defaultValue
		
	def read_state_variable_default(self, name, defaultValue):
		print "The method 'read_state_variable_default' is deprecated. Please use 'read_state_variable(name, defaultValue)"
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
