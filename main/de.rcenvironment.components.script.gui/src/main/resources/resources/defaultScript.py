# You can read inputs with
# 
#       RCE.read_input(String input_name)
# 
# You can write outputs with
# 
#       RCE.write_output(String output_name, OutputDataType value)
#
# Thereby, the type (OutputDataType) of the value must fit the data type of the output (as defined in the tab Inputs/Outputs). File and Directory are represented by the absolute file paths.
#
# (Note: The module RCE used is already imported in the script during execution.)
#
# Examples:
# - If you like to double an incoming value (x is an input of type Integer and y an output of type Integer):
#       RCE.write_output("y", 2 * RCE.read_input("x"))
# - If you like to access an incoming file (f_in is an input of type File):
#       file = open(RCE.read_input("f_in"),"r")
# - If you like to send a file to an output (f_out is an output of type File):
#       absolute_file_path = /home/user_1/my_file.txt
#       RCE.write_output("f_out", absolute_file_path)
#

import sys
 
sys.stderr.write('Script was not configured')
sys.stderr.flush()