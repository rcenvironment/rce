import socket
import traceback
import sys
import RCE_Channel as RCE
import json

class SocketReader:
    def __init__(self, socket):
        self._socket = socket
        self._nonreturned_messages = []

    def getNextMessage(self):
        if not len(self._nonreturned_messages) == 0:
            return self._pop_nonreturned_message()

        while len(self._nonreturned_messages) == 0:
            data = self._socket.recv(1024)
            data_buffer = bytearray()
            for byte in data:
                if byte != 0:
                    data_buffer.append(byte)
                else:
                    self._nonreturned_messages.append(bytes(data_buffer).decode('UTF-8'))
                    data_buffer = bytearray()

        return self._pop_nonreturned_message()
    
    def _pop_nonreturned_message(self):
        return_value = self._nonreturned_messages[0]
        self._nonreturned_messages = self._nonreturned_messages[1:]
        return return_value


class Worker(object):

    def __init__(self):
        self.sock = None
        self.port = None
        self.encoding = 'UTF-8'
        self.number = None

    #Each worker is given a number to distinguish them later (e.g. when writing in the LOG).
    def setNumber(self, number):
        self.number = number

    #Sets the port the socket will be bound.
    def setPort(self, port):
        self.port = int(port)

    def createSocket(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self._socket_reader = SocketReader(self.sock)

    def connectSocket(self):
        self.sock.connect(("localhost", self.port))
        data = self.receiveMessage()

    #Sends the authentification token to RCE. The return value depends on the answer received by RCE.
    def registerPythonInstance(self, token):
        self.sendMessage(str(token))
        reply = self.receiveMessage()
        if reply == "Token accepted.":
            return True
        elif reply == "Token declined.":
            return False

    #Is called after the worker has been initialized. Waits for RCE to send an order.
    def waitForOrder(self):
        order = self.receiveMessage()
        socketState=""
        while order != "stopInstance":
            if order == "executeUserscript":
                try:
                    socketState = self.executeUserscriptJSON()
                except:
                    pass
                if socketState == "socket closed":
                    return
            order = self.receiveMessage()
            if order == None:
                return
        self.stopInstance()

    #Is called whenever a script shall be executed within a workflow in RCE.
    def executeUserscript(self):
        #Receive the number of lines of code within the script which shall be executed.
        self.sendMessage("Ready to receive the script")
        scriptLength = int(self.receiveMessage())
        if scriptLength == None:
            return "socket closed"
        self.sendMessage("Received script length")

        #Receive the script. Every line is received separately.
        script = ""
        for e in range(scriptLength):
            self.sendMessage("Send next line.")
            inputString = self.receiveMessage()
            if inputString == None:
                return "socket closed"
            script += inputString + "\n"

        #Execute the script
        RCE.init_module()
        CONTEXT = {"RCE":RCE}
        executionError = False
        try:
            exec(script, CONTEXT)
        except: 
            traceback.print_exc()
            executionError = True
        RCE.writeoutput_internal()

        #Send an error if the script execution failed.
        if not executionError:
            self.sendMessage("Finished script execution successfully. Waiting for next task.")
        else:
            self.sendMessage("Error when executing the script. Waiting for next task.")
        return "socket is still open"
    
    def executeUserscriptJSON(self):
        #Receive the number of lines of code within the script which shall be executed.
        self.sendMessage("Ready to receive the script")

        #Receive the script in JSON format

        inputString = self.receiveMessage()

        if inputString == None:
            print( "socket closed")
         
        #convert received JSON String into dictionary   
        
        #print("Before Loads: "+inputString )
        parsedJSON=json.loads(inputString)
        #print(parsedJSON)
               
        if parsedJSON["pythonCommand"]!= "execute":
            self.sendMessage("Error when executing the script. Waiting for next task.")
            return "socket is still open"  
        
        #Execute the script
        RCE.init_module()
        CONTEXT = {"RCE":RCE}
        executionError = False
        try:
            codeObject = compile(parsedJSON["script"], 'script', 'exec')
            exec(codeObject, CONTEXT)
            
        except: 
            traceback.print_exc()
            executionError = True

        try:
            RCE.writeoutput_internal()
        except:
            self.sendMessage("Error when writing output to temporary folder.")
            executionError = True

        #Send an error if the script execution failed.
        if not executionError:
            self.sendMessage("Finished script execution successfully. Waiting for next task.")
        else:
            self.sendMessage("Error when executing the script. Waiting for next task.")
        return "socket is still open"
        
    def stopInstance(self):
        self.sendMessage("Shutting down instance.")
        self.sock.close()

    def receiveMessage(self):
        return self._socket_reader.getNextMessage()

    def sendMessage(self, string):
        #print("Sending " + str(string))
        array = bytearray(string.encode('UTF-8'))
        # We terminate each sent message with a null byte
        array.append(0)
        self.sock.send(bytes(array))

def start(port, token, number):
    #Create and initialize a worker object.
    worker = Worker()
    worker.setNumber(number)
    worker.setPort(port)
    worker.createSocket()
    #Connect to RCE.
    worker.connectSocket()
    #Send authentification token.
    registrationWasSuccessful = worker.registerPythonInstance(token)
    if not registrationWasSuccessful:
        worker.stopInstance()
    worker.waitForOrder()

#This statement is executed when the script is started.            
if __name__ == "__main__":
    port = sys.argv[1]
    token = sys.argv[2]
    start(port, token, 1)
