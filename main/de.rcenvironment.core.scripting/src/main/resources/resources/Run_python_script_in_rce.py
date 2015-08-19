'''
Created on 24.04.2013
Main script for running a user defined script with RCE
@author: Sascha Zur
'''
import RCE_Channel as RCE

CONTEXT = {"RCE" :  RCE}
USERSCRIPT = open("userscript.py", "r").read()
exec(USERSCRIPT, CONTEXT)
RCE.writeoutput_internal()
