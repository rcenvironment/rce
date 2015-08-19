/*
 * Copyright (C) 2006-2011 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.optimizer.simulator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * This class is to provide a jar file which the dakota script will run. 
 * Here the values from dakota and rce are linked.
 * @author Sascha Zur
 */
public class DakotaResponse {
    private final int standardPort = 10000;  
    private final int bufferSize = 200;
    
    /**
     * This methods does the handshake with rce.
     *
     * @param path :
     * @param data :
     * @param fw :
     * @throws IOException :
     */
    public void respond(String path, String data, BufferedWriter fw) throws IOException {
        String ip = "127.0.0.1"; // localhost
        fw.append("Reading port ... \r\n");        
        fw.flush();
        int port = readPort(path, fw);
        fw.append("done: " + port + "\r\n");
        fw.append("Open Connection ... \r\n");
        fw.flush();
        Socket socket = new Socket(ip, port); // verbindet sich mit Server
        fw.append("done.\r\n");
        fw.flush();
        fw.append("Writing message for:" + path + "/" + data + " ...");
        writeMessagetoDakota(socket, path + "&&" + data + "\r\n");
        fw.append("done.\r\n");
        fw.flush();
        fw.append("blocking for answer ... ");
        fw.flush();
        blockUntilMessageArrives(socket);
        fw.append("done. \r\n Closing Socket");
        fw.flush();
        socket.close();
    }
    private int readPort(String path, BufferedWriter fw) throws IOException {
        fw.flush();
        File portfile = null;
        try {
            File f = null;
            fw.append("Path : " + path);
            if (path.contains(".inworkdir")){
                f = new File(path.substring(0, path.lastIndexOf(File.separator)));
            } else {
                f = new File(path.substring(0, path.indexOf("workdir")));
            }
            fw.append("F : " + f.getAbsolutePath());

            for (File subFile : f.listFiles()){
                fw.append(subFile.getAbsolutePath());
                if (subFile.getName().endsWith(".port")){
                    portfile = subFile;
                }
            }
                
        } catch (NullPointerException e){
            fw.append(e.getMessage());
            fw.flush();
        }
        BufferedReader fr;
        fw.flush();
        try {
            fw.flush();
            fr = new BufferedReader(new FileReader(portfile));
            String firstLine =  fr.readLine();
            fw.flush();
            return Integer.valueOf(firstLine);

        } catch (FileNotFoundException e) {
            fw.append(e.getMessage());
            fr = null;
        } catch (IOException e) {
            fw.append(e.getMessage());

            return standardPort;
        }
        fw.flush();
        return standardPort;
    }
    private void writeMessagetoDakota(java.net.Socket socket, String nachricht) throws IOException {
        PrintWriter printWriter =
                new PrintWriter(
                        new OutputStreamWriter(
                                socket.getOutputStream()));
        printWriter.print(nachricht);
        printWriter.flush();

    }
    private void blockUntilMessageArrives(java.net.Socket socket) throws IOException {
        BufferedReader bufferedReader =
                new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream()));
        char[] buffer = new char[bufferSize];
        bufferedReader.read(buffer, 0, bufferSize); // blockiert bis Nachricht empfangen
    }

    /**
     * Main. 
     * @param args : Program arguments
     */
    public static void main(String[] args) {
        try {
            BufferedWriter fw = new BufferedWriter(new FileWriter(new File("LOGGER")));
            fw.append("My arguments: " + args[0] + "   " + args[1]);
            DakotaResponse client = new DakotaResponse();
            try {
                client.respond(args[0], args[1], fw);
                fw.flush();
                fw.close();
            } catch (IOException e) {
                fw.append(e.toString());
                fw.append(e.getCause().toString());
                fw.append(e.getMessage());
                fw.flush();
                fw.close();
            }
        } catch (IOException e) {
            System.err.println(e);

        }
    }

}
