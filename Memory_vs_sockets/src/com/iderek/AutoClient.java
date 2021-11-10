package com.iderek;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class AutoClient {
    public static void main(String[] args) {
        String action = null;

        int repeatNumber = 1000;

        double averageTime;
        String connectionInfo;
        String memoryType;
        String message;
        String fileName;
        byte[] byteMessage;
        int length;
        byte[] b;
        String path;
        boolean useHeaders;
        boolean serverCheck = false;

        ClientHelper tester = new ClientHelper();

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter host name:  ");
        String hostName = scanner.nextLine();
        System.out.print("Enter port number:  ");
        int portNumber = scanner.nextInt();
        scanner.nextLine();
        System.out.print("Enter memory type name:  ");
        memoryType= scanner.nextLine();
        System.out.print("Where do you want to save test files? ");
        path= scanner.nextLine();
        System.out.print("Enter test name: ");
        String testName= scanner.nextLine();

        System.out.print("Use headers? (y/n)");
        useHeaders = scanner.nextLine().equals("y");

        if (useHeaders){
            System.out.print("Use server check? (y/n)");
            serverCheck = scanner.nextLine().equals("y");
        }

        byte[] dataBytes = null;
        try (Socket serverSocket = new Socket(hostName, portNumber);
        ){
            serverSocket.setTcpNoDelay(true);
            connectionInfo = serverSocket.toString();
            System.out.println("Connected to: " + connectionInfo);

            b = ByteBuffer.allocate(4).putInt(useHeaders ? 1 : 0).array();
            serverSocket.getOutputStream().write(b, 0, 4);

            if(useHeaders){
                b = ByteBuffer.allocate(4).putInt(serverCheck ? 1 : 0).array();
                serverSocket.getOutputStream().write(b, 0, 4);
            }

            for (int step = 1; step <= 32768 ; step *= 2)
                {
                int fileSize = 1024 * step ;
                dataBytes = tester.generateFile(fileSize);
                fileName = "test";


                System.out.println("begining test :" + String.valueOf(step) + "kB");


                System.out.println("Sending to server");
                averageTime = System.currentTimeMillis();
                for (int x = 0; x < repeatNumber; x++) {
                    if(!useHeaders)
                        tester.sendFileToServer(dataBytes,fileName, serverSocket) ;
                    else
                        tester.sendFileToServer2(dataBytes,fileName, serverSocket, serverCheck);
                }
                averageTime = (System.currentTimeMillis() - averageTime)/repeatNumber;


                tester.saveResult(fileSize, averageTime,testName, hostName.toLowerCase().equals("localhost")?"local net":"net","server write");


                System.out.println("Receiving from server");
                ClientHelper.FileTestData testData = null;
                averageTime = System.currentTimeMillis();
                for (int x = 0; x < repeatNumber; x++) {
                    if(!useHeaders)
                        testData = tester.reachFileFromServer(serverSocket, fileName);
                    else
                        testData = tester.reachFileFromServer2(serverSocket, fileName);
                    dataBytes = testData.data;
                }
                averageTime = (System.currentTimeMillis() - averageTime)/repeatNumber;
                tester.saveResult(fileSize, averageTime,testName, hostName.toLowerCase()
                        .equals("localhost")?"local net":"net","server read");


                System.out.println("Saving to memory");
                averageTime = System.currentTimeMillis();
                for (int x = 0; x < repeatNumber; x++) {
                    tester.saveFileToMemory(dataBytes, fileName, path);
                }
                averageTime = (System.currentTimeMillis() - averageTime)/repeatNumber;
                tester.saveResult(fileSize, averageTime,testName, memoryType,"memory write");


                System.out.println("Reading from memory");
                averageTime = System.currentTimeMillis();
                for (int x = 0; x < repeatNumber; x++) {
                    testData = tester.reachFromMemory(fileName, path);
                    dataBytes = testData.data;
                }
                averageTime = (System.currentTimeMillis() - averageTime)/repeatNumber;
                tester.saveResult(fileSize, averageTime,testName, memoryType,"memory read");

            }

            OutputStream out = serverSocket.getOutputStream();
            if (!useHeaders) {
                message = "end";
                byteMessage = message.getBytes();
                length = byteMessage.length;
                b = ByteBuffer.allocate(4).putInt(length).array();
                out.write(b);
                out.write(byteMessage);
            }else {
                Header header = new Header(SocketMessageHelper.end, "",0);
                out.write(header.getByteHeader());
            }
            serverSocket.close();

        }catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            e.printStackTrace();
            System.exit(1);
        }catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + hostName);
            e.printStackTrace();
            System.exit(1);
        }catch (InterruptedException e){
            e.printStackTrace();
            System.exit(1);
        }

    }
}
