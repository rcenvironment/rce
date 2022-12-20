'''
Created on 02.03.2020

@author: Kathrin Schaffert (#17088)
'''

from __future__ import with_statement
import os

dictionary = dict

class InputFileFactory(dictionary):

    COMMENT = "comment"
    d = dict
    p = ""
    written_files = []
	
    def __init__(self,dictionary,rce_input_files):
        self.d = dictionary
        self.written_files = rce_input_files

    def add_variable(self,name,value):
        if type(name) is str:
            self.d[name] = value
        else:
            raise NameError("The specified variable name " + str(name) + " is not of type string.'")

    def add_dictionary(self,name):
        if type(name) is str:
            self.d[name] = dict()
        else:
            raise NameError("The specified dictionary name '" + str(name) + "' is not of type String.'")

    def add_value_to_dictionary(self,name,key,value):
        if name in self.d:
            self.d[name][key] = value
        else:
            raise NameError("The specified dictionary'" + str(name) + "' is not defined. Please generate a dictionary beforehand.'")

    def add_comment(self,value):
        self.d[value] = self.COMMENT

    def write_to_file(self,name,overwrite=False):
        head = os.path.split(name)[0]
        tail = os.path.split(name)[1]
        
        if head:	
        	raise ValueError("It is not allowed to define an absolute or relative path for '" + str(name) + 
        	"' Please choose a simple string for your file name.'" +
        	"' The file will be stored in the working directory, tool directory or temp directory depending on the settings and can be copied into your local directory via Python commands, if neccessary.'")
        
        path = os.path.join(self.p,tail)
        if not overwrite and os.path.exists(path):
            dir = os.path.dirname(path)
            raise ValueError("The file '" + str(name) + "' already exists in '" + str(dir) + "'")
        else:
            with open(path,'w') as f:
                for key in self.d:
                    if self.d[key] == self.COMMENT:
                        f.write("# " + str(key) + "\n")
                    else:
                        f.write(key + " = " + str(self.d[key]) + "\n")
                self.written_files.append(path)
            return path            
