package com.iderek;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ClientHelper {
    int defaultBufferSize = 524288;
    SocketMessageHelper messageHelper;

    public ClientHelper(){
        messageHelper = new SocketMessageHelper();
    }

    public ArrayList<String> reachFileNamesFromServer(Socket server) throws IOException{
        InputStream in = server.getInputStream();
        OutputStream out = server.getOutputStream();

        String message = "viewing";
        byte[] byteMessage = message.getBytes();
        int length = byteMessage.length;
        byte[] b = ByteBuffer.allocate(4).putInt(length).array();
        out.write(b);
        out.write(byteMessage);

        b = new byte[4];
        in.read(b);
        int ok = ByteBuffer.wrap(b).getInt();

        b = new byte[4];
        in.read(b);
        int arrayLength = ByteBuffer.wrap(b).getInt();

        ArrayList<String> fileNames = new ArrayList<String>();
        for (int i = 0; i<arrayLength; i++){
            b = new byte[4];
            in.read(b);
            length = ByteBuffer.wrap(b).getInt();
            byteMessage = new byte[length];
            in.read(byteMessage);
            String name = new String(byteMessage, "UTF-8");
            fileNames.add(name);
        }

        return fileNames;
    }

    public ArrayList<String> reachFileNamesFromServer2(Socket server) throws IOException{
        InputStream in = server.getInputStream();
        OutputStream out = server.getOutputStream();

        Header header = new Header(SocketMessageHelper.viewing, "", 0);
        out.write(header.getByteHeader());

        byte[] b = new byte[4];
        in.read(b);
        int arrayLength = ByteBuffer.wrap(b).getInt();
        ArrayList<String> fileNames = new ArrayList<String>();
        for (int i = 0; i<arrayLength; i++){
            b = new byte[4];
            in.read(b);
            int length = ByteBuffer.wrap(b).getInt();
            byte[] byteMessage = new byte[length];
            in.read(byteMessage);
            String name = new String(byteMessage, "UTF-8");
            fileNames.add(name);
        }

        return fileNames;
    }

    public void sendFileToServer2(byte[] dataBytes,String fileName, Socket server, boolean serverCheck) throws IOException, InterruptedException{

        InputStream in = server.getInputStream();
        OutputStream out = server.getOutputStream();


        int length = dataBytes.length;

        Header header = new Header(SocketMessageHelper.sending, fileName, length);
        out.write(header.getByteHeader());


        ByteArrayInputStream byteStream = new ByteArrayInputStream(dataBytes);

        int bufferSize;
        if (length> defaultBufferSize)
            bufferSize = defaultBufferSize;
        else
            bufferSize = length;

        byte[]buffer = new byte[bufferSize];


        int count;
        while ((count = byteStream.read(buffer)) > 0 && server.isConnected()) {
            out.write(buffer, 0, count);
        }

        if (serverCheck) {
            byte[] b = new byte[1];
            in.read(b, 0, 1);
            if (b[0] != 1)
                throw new IOException("Server not ok!");
        }
        return;
    }

    public void sendFileToServer(byte[] dataBytes,String fileName, Socket server) throws IOException, InterruptedException{

        InputStream in = server.getInputStream();
        OutputStream out = server.getOutputStream();

        String message = "sending";
        byte[] byteMessage = message.getBytes();
        int length = byteMessage.length;
        byte[] b = ByteBuffer.allocate(4).putInt(length).array();
        out.write(b);
        out.write(byteMessage);

        b = new byte[4];
        in.read(b);
        int ok = ByteBuffer.wrap(b).getInt();

        message = fileName;
        byteMessage = message.getBytes();
        length = byteMessage.length;
        b = ByteBuffer.allocate(4).putInt(length).array();
        out.write(b);
        out.write(byteMessage);


        ByteArrayInputStream byteStream = new ByteArrayInputStream(dataBytes);
        length = dataBytes.length;

        int bufferSize;
        if (length> defaultBufferSize)
            bufferSize = defaultBufferSize;
        else
            bufferSize = length;

        b = ByteBuffer.allocate(4).putInt(length).array();
        out.write(b);

        byte[]buffer = new byte[bufferSize];

        b = new byte[4];
        in.read(b);
        ok = ByteBuffer.wrap(b).getInt();
        int count;
        while ((count = byteStream.read(buffer)) > 0 && server.isConnected()) {
            out.write(buffer, 0, count);
        }

        b = new byte[4];
        in.read(b);
        length = ByteBuffer.wrap(b).getInt();
        byteMessage = new byte[length];
        in.read(byteMessage);
        String serverMessage = new String(byteMessage, "UTF-8");
        if (!serverMessage.equals("recieved"))
            throw new SocketException(serverMessage);
        System.out.println("Server: " +  serverMessage);

        return;
    }

    public class FileTestData{
        public String name;
        public byte[] data;

        public FileTestData(){}

        public FileTestData(String name, byte[] data){
            this.name = name;
            this.data = data;
        }
    }

    public FileTestData reachFileFromServer2(Socket server, String fileName) throws IOException, InterruptedException{
        InputStream in = server.getInputStream();
        OutputStream out = server.getOutputStream();

        Header header = new Header(SocketMessageHelper.requesting, fileName, 0);
        out.write(header.getByteHeader());

        byte []b = new byte[4];
        in.read(b, 0,4);
        int length = ByteBuffer.wrap(b).getInt();

        int bufferSize;
        if (length> defaultBufferSize)
            bufferSize = defaultBufferSize;
        else
            bufferSize = length;


        byte[] buffer = new byte[bufferSize];
        byte[] bytes;
        ByteBuffer dataBuffer = ByteBuffer.allocate(length + length);
        int n = length;
        int dataRead;
        while ((dataRead = in.read(buffer, 0, bufferSize)) != -1) {
            n -= dataRead;
            dataBuffer.put(buffer,0, dataRead);
            if (n <= 0)
                break;
            if (n <= bufferSize)
                bufferSize = n;
        }

        bytes = Arrays.copyOfRange(dataBuffer.array(), 0,length);

        dataBuffer.clear();

        return new FileTestData(fileName, bytes);
    }

    public FileTestData reachFileFromServer(Socket server, String fileName) throws IOException, InterruptedException{
        InputStream in = server.getInputStream();
        OutputStream out = server.getOutputStream();

        String message = "requesting";
        byte []byteMessage = message.getBytes();
        int length = byteMessage.length;
        byte[] b = ByteBuffer.allocate(4).putInt(length).array();
        out.write(b);
        out.write(byteMessage);

        b = new byte[4];
        in.read(b);
        int ok = ByteBuffer.wrap(b).getInt();

        byteMessage = fileName.getBytes();
        length = byteMessage.length;
        b = ByteBuffer.allocate(4).putInt(length).array();
        out.write(b);
        out.write(byteMessage);

        b = new byte[4];
        in.read(b);
        length = ByteBuffer.wrap(b).getInt();
        byteMessage = new byte[length];
        in.read(byteMessage);
        String serverMessage = new String(byteMessage, "UTF-8");
        System.out.println("Server: " +  serverMessage);

        b = new byte[4];
        in.read(b);
        length = ByteBuffer.wrap(b).getInt();

        int bufferSize;
        if (length> defaultBufferSize)
            bufferSize = defaultBufferSize;
        else
            bufferSize = length;

        b = new byte[4];
        in.read(b);
        ok = ByteBuffer.wrap(b).getInt();
        Thread.sleep(1);
        byte[] buffer = new byte[bufferSize];
        byte[] bytes = new byte[length];
        ByteBuffer dataBuffer = ByteBuffer.allocate(length + length);
        String fromBytes = new String();
        int n = length;
        int dataRead;
        while ((dataRead = in.read(buffer,0, bufferSize)) != -1) {
            n -= dataRead;
            dataBuffer.put(buffer,0, dataRead);
            if (n <= 0)
                break;
            if (n <= bufferSize)
                bufferSize = n;
        }

        bytes = Arrays.copyOfRange(dataBuffer.array(), 0,length);

        dataBuffer.clear();

        message = "recieved";
        byteMessage = message.getBytes();
        length = byteMessage.length;
        b = ByteBuffer.allocate(4).putInt(length).array();
        out.write(b);
        out.write(byteMessage);

        return new FileTestData(fileName, bytes);
    }

    public void saveFileToMemory(byte[] dataBytes,String fileName, String path) throws IOException{
        Path file = Paths.get(path+"/"+fileName);
        try{
            Files.write(file, dataBytes);
            return;
        }catch (IOException e){
            System.out.println(e.toString());
        }catch ( NullPointerException a){
            System.out.println("data not initialised!");
        }
        return ;
    }

    public FileTestData reachFromMemory(String fileName, String path){

        Path nFile = Paths.get(path+"/"+fileName);
        try{
            byte[] dataBytes = Files.readAllBytes(nFile);
            return new FileTestData(fileName, dataBytes);
        }catch (IOException e){
            System.out.println(e.toString());
            e.printStackTrace();
        }catch ( NullPointerException a){
            System.out.println("data not initialised!");
            a.printStackTrace();
        }
        return null;
    }

    public void saveResult(int fileSize, double transferTime, String testName, String transferMedia, String action){
        String results = "\n"
                + new Date(System.currentTimeMillis())
                + ";" + action
                + ";" + fileSize
                + ";" + String.format("%.3f",transferTime)
                + ";" + transferMedia
                + ";" + testName+";;;";

        try{
            String resultsFile= "Results.csv";
            FileWriter fw = new FileWriter(resultsFile,true);
            fw.write(results);
            fw.close();
        }
        catch(IOException ioe){
            System.out.println("IOException: " + ioe.getMessage());
        }
    }

    public byte[] generateFile(int fileSize){
        byte[] dataBytes = new byte[fileSize];
        new Random().nextBytes(dataBytes);
        return dataBytes;
    }
}
