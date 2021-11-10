package com.iderek;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Header {
    public byte message;
    public String name;
    public Integer length;

    public Header(byte message, String name, Integer length) {
        this.message = message;
        this.name = name;
        this.length = length;
    }
    public Header(byte[] byteHeader) throws IOException{
        message = byteHeader[0];
        byte[] byteName = new byte[50];
        System.arraycopy(byteHeader, 1, byteName, 0, 50);
        name = new String(byteName, "UTF-8").trim();
        byte[] byteLength = new byte[4];
        System.arraycopy(byteHeader, 51, byteLength, 0,4);
        length = ByteBuffer.wrap(byteLength).getInt();

    }

    public byte[] getByteHeader()
    {
        byte[] byteHeader = new byte[55];
        byteHeader[0] = message;
        System.arraycopy(name.getBytes(), 0, byteHeader, 1, name.getBytes().length);
        System.arraycopy(ByteBuffer.allocate(4).putInt(length).array(), 0, byteHeader, 51, 4);
        return byteHeader;
    }
}