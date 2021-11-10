package com.iderek;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;


public class Server {
    public static void main(String[] args) {
        int bufferSize;
        boolean useHeaders = false;
        boolean serverCheck = false;
        int defaultBufferSize = 524288;

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter port number:  ");
        int portNumber = scanner.nextInt();

        try (ServerSocket serverSocket = new ServerSocket(portNumber)
        ) {
            System.out.println("Waiting for connection at: " + InetAddress.getLocalHost().getHostAddress());
            Socket clientSocket = serverSocket.accept();
            clientSocket.setTcpNoDelay(true);
            OutputStream out = clientSocket.getOutputStream();
            InputStream in =clientSocket.getInputStream();

            System.out.println("Client connected on port " + portNumber +". Servicing requests.");

            ArrayList<String> fileNames = new ArrayList<String>();
            ArrayList<byte[]> files = new ArrayList<>();
            boolean end = false;

            byte[] b = new byte[4];
            in.read(b, 0, 4);
            useHeaders = ByteBuffer.wrap(b).getInt() == 1;

            if(useHeaders){
                b = new byte[4];
                in.read(b, 0, 4);
                serverCheck = ByteBuffer.wrap(b).getInt() == 1;
            }

            while (clientSocket.isConnected() && !end) {
                if (!useHeaders){
                    b = new byte[4];
                    in.read(b);
                    int length = ByteBuffer.wrap(b).getInt();
                    byte[] byteMessage = new byte[length];
                    in.read(byteMessage);
                    String clientMessage = new String(byteMessage, "UTF-8");
                    System.out.println("Client: " + clientMessage);
                    String message;
                    switch (clientMessage){
                        case "sending":
                            b = ByteBuffer.allocate(4).putInt(1).array(); //ok
                            out.write(b);

                            b = new byte[4];
                            in.read(b);
                            length = ByteBuffer.wrap(b).getInt();
                            byteMessage = new byte[length];
                            in.read(byteMessage);
                            String fileName = new String(byteMessage, "UTF-8");

                            b = new byte[4];
                            in.read(b);
                            length = ByteBuffer.wrap(b).getInt();

                            if (length> defaultBufferSize)
                                bufferSize = defaultBufferSize;
                            else
                                bufferSize = length;

                            b = ByteBuffer.allocate(4).putInt(1).array(); //ok
                            out.write(b);

                            byte[] dataBytes;
                            ByteBuffer dataBuffer;
                            byte[] buffer;

                            buffer = new byte[bufferSize];
                            dataBuffer = ByteBuffer.allocate(length + length);
                            long n = length;
                            int dataRead;
                            while ((dataRead = in.read(buffer)) != -1) {
                                n -= dataRead;
                                dataBuffer.put(buffer,0,dataRead);
                                if (n <= 0)
                                    break;
                            }

                            dataBytes = Arrays.copyOfRange(dataBuffer.array(), 0,length);

                            message = "recieved";
                            byteMessage = message.getBytes();
                            length = byteMessage.length;
                            b = ByteBuffer.allocate(4).putInt(length).array();
                            out.write(b);
                            out.write(byteMessage);

                            dataBuffer.clear();

                            System.out.println("Received " + dataBytes.length / 1024 + " kB from: " + clientSocket.toString());

                            if (fileNames.contains(fileName)) {
                                files.set(fileNames.indexOf(fileName), dataBytes);
                            }else {
                                fileNames.add(fileName);
                                files.add(dataBytes);
                            }
                            dataBuffer.clear();
                            break;
                        case "viewing":
                            b = ByteBuffer.allocate(4).putInt(1).array(); //ok
                            out.write(b);

                            byteMessage = ByteBuffer.allocate(4).putInt(fileNames.size()).array();
                            out.write(byteMessage);

                            for (String name:fileNames) {
                                byteMessage = name.getBytes();
                                length = byteMessage.length;
                                b = ByteBuffer.allocate(4).putInt(length).array();
                                out.write(b);
                                out.write(byteMessage);
                            }
                            break;
                        case "requesting":
                            b = ByteBuffer.allocate(4).putInt(1).array(); //ok
                            out.write(b);


                            b = new byte[4];
                            in.read(b);
                            length = ByteBuffer.wrap(b).getInt();
                            byteMessage = new byte[length];
                            in.read(byteMessage);
                            fileName = new String(byteMessage, "UTF-8");

                            int i = fileNames.indexOf(fileName);
                            dataBytes = files.get(i);

                            message = "sending " + fileName;
                            byteMessage = message.getBytes();
                            length = byteMessage.length;
                            b = ByteBuffer.allocate(4).putInt(length).array();
                            out.write(b);
                            out.write(byteMessage);

                            length = dataBytes.length;
                            b = ByteBuffer.allocate(4).putInt(length).array();
                            out.write(b);

                            if (length> defaultBufferSize)
                                bufferSize = defaultBufferSize;
                            else
                                bufferSize = length;

                            b = ByteBuffer.allocate(4).putInt(1).array(); //ok
                            out.write(b);

                            buffer = new byte[bufferSize];
                            ByteArrayInputStream byteStream = new ByteArrayInputStream(dataBytes);
                            int count;
                            while ((count = byteStream.read(buffer)) > 0) {
                                out.write(buffer, 0, count);
                            }


                            System.out.println("Sent");

                            b = new byte[4];
                            in.read(b);
                            length = ByteBuffer.wrap(b).getInt();
                            byteMessage = new byte[length];
                            in.read(byteMessage);
                            clientMessage = new String(byteMessage, "UTF-8");
                            System.out.println("Client: " + clientMessage);



                            break;
                        case "end":
                            end = true;
                            break;
                        default:
                            message = "Unrecognised command";
                            byteMessage = message.getBytes();
                            length = byteMessage.length;
                            b = ByteBuffer.allocate(4).putInt(length).array();
                            out.write(b);
                            out.write(byteMessage);
                            continue;
                    }
                }else {
                    byte[] byteHeader;
                    ByteBuffer dataBuffer;
                    byte[] buffer;

                    buffer = new byte[55];
                    dataBuffer = ByteBuffer.allocate(100);
                    int n = 55;
                    int dataRead;
                    int toRead = 55;
                    while ((dataRead = in.read(buffer,0,toRead)) != -1) {
                        n -= dataRead;
                        dataBuffer.put(buffer,0,dataRead);
                        if (n <= 0)
                            break;
                        if (n<=55)
                            toRead = n;
                    }

                    byteHeader = Arrays.copyOfRange(dataBuffer.array(), 0,55);

                    Header header = new Header(byteHeader);

                    switch (header.message){
                        case SocketMessageHelper.sending:
                            if (header.length> defaultBufferSize)
                                bufferSize = defaultBufferSize;
                            else
                                bufferSize = header.length;


                            buffer = new byte[bufferSize];
                            byte[] bytes;
                            dataBuffer = ByteBuffer.allocate(header.length + header.length);
                            n = header.length;
                            while ((dataRead = in.read(buffer, 0, bufferSize)) != -1) {
                                n -= dataRead;
                                dataBuffer.put(buffer,0, dataRead);
                                if (n <= 0)
                                    break;
                                if (n <= bufferSize)
                                    bufferSize = n;
                            }

                            bytes = Arrays.copyOfRange(dataBuffer.array(), 0,header.length);

                            if (fileNames.contains(header.name)) {
                                files.set(fileNames.indexOf(header.name), bytes);
                            }else {
                                fileNames.add(header.name);
                                files.add(bytes);
                            }
                            if (serverCheck){
                                b = new byte[1];
                                b[0] = 1;
                                out.write(b, 0, 1);
                            }
                            dataBuffer.clear();
                            break;
                        case SocketMessageHelper.requesting:
                            int i = fileNames.indexOf(header.name);
                            bytes = files.get(i);

                            int length = bytes.length;
                            b = ByteBuffer.allocate(4).putInt(length).array();
                            out.write(b);

                            ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);

                            if (length> defaultBufferSize)
                                bufferSize = defaultBufferSize;
                            else
                                bufferSize = length;

                            buffer = new byte[bufferSize];

                            int count;
                            while ((count = byteStream.read(buffer)) > 0 && clientSocket.isConnected()) {
                                out.write(buffer, 0, count);
                            }

                            break;
                        case SocketMessageHelper.viewing:

                            byte[] byteMessage = ByteBuffer.allocate(4).putInt(fileNames.size()).array();
                            out.write(byteMessage);

                            for (String name:fileNames) {
                                byteMessage = name.getBytes();
                                length = byteMessage.length;
                                b = ByteBuffer.allocate(4).putInt(length).array();
                                out.write(b);
                                out.write(byteMessage);
                            }
                            break;
                        case SocketMessageHelper.end:
                            end = true;
                            break;
                        default:
                            throw new IOException("Unrecognised input command!");
                    }
                }

            }

        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                + portNumber + " or listening for a connection");
            e.printStackTrace();
        }
    }
}
