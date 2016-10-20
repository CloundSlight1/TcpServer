package com.wuyz.tcpserver;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Locale;

public class Main {

    static final char[] HEX_CHAR = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static String name;
    private static String sha1;
    private static long length;

    public static void main(final String[] args) {
        if (args == null || args.length != 1) {
            System.err.println("Usage: ServerTcp file");
            return;
        }

        final File file = new File(args[0]);
        if (!file.isFile()) {
            System.err.println("File not exist: " + args[0]);
            return;
        }

        name = file.getName();
        length = file.length();
        try (FileInputStream inputStream = new FileInputStream(file)) {
            sha1 = getSHA1(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        System.out.println(String.format(Locale.getDefault(), "name: %s, length: %s(0x%X), sha1: %s",
                name, getLengthDesc(length), length, sha1));

        try {
            ServerSocket serverSocket = new ServerSocket(9999, 10);
            System.out.println("ip: " + InetAddress.getLocalHost().getHostAddress() + ":" + serverSocket.getLocalPort());
            while (true) {
                System.out.println("Waiting for client ...");
                final Socket socket = serverSocket.accept();
                System.out.println("Accept client: " + socket.getLocalAddress().toString());
                new Thread(() -> {
                    try {
                        InputStream inputStream = socket.getInputStream();
                        OutputStream outputStream = socket.getOutputStream();
                        byte[] buffer;
                        while (true) {
                            int n = inputStream.read();
//                                System.out.println("receive: " + n);
                            switch (n) {
                                case 0:
                                    if (!file.isFile()) {
                                        System.err.println("File not exist: " + args[0]);
                                        return;
                                    }
                                    sendFileInfo(outputStream);
                                    outputStream.flush();
                                    outputStream.close();
                                    break;
                                case 1:
                                    if (!file.isFile()) {
                                        System.err.println("File not exist: " + args[0]);
                                        return;
                                    }
                                    buffer = new byte[2048];
                                    try (FileInputStream fileInputStream = new FileInputStream(file)) {
                                        System.out.println("send file begin");
                                        while ((n = fileInputStream.read(buffer)) > 0) {
//                                            System.out.println("n = " + n);
                                            outputStream.write(buffer, 0, n);
                                        }
                                        System.out.println("send file end");
                                    }
                                    outputStream.flush();
                                    outputStream.close();
                                    break;
                            }
                            Thread.sleep(500);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Server stopped!");
        return;
    }

    static String getLengthDesc(float length) {
        if (length < 1024)
            return length + "B";

        length /= 1024;
        if (length < 1024)
            return String.format(Locale.getDefault(), "%.1fK", length);

        length /= 1024;
        if (length < 1024)
            return String.format(Locale.getDefault(), "%.1fM", length);

        length /= 1024;
        return String.format(Locale.getDefault(), "%.1fG", length);
    }

    public static String getSHA1(String input) {
        if (input == null || input.isEmpty())
            return null;
        try {
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(input.getBytes());
            byte[] buffer = mdInst.digest();
            char str[] = new char[buffer.length << 1];
            for (int i = 0; i < buffer.length; i++) {
                byte b = buffer[i];
                str[2 * i] = HEX_CHAR[b >>> 4 & 0xf];
                str[2 * i + 1] = HEX_CHAR[b & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getSHA1(InputStream input) {
        byte[] data = new byte[1024];
        int n;
        try {
            MessageDigest mdInst = MessageDigest.getInstance("SHA-1");
            while ((n = input.read(data)) > 0) {
                mdInst.update(data, 0, n);
            }
            byte[] buffer = mdInst.digest();
            char str[] = new char[buffer.length << 1];
            for (int i = 0; i < buffer.length; i++) {
                byte b = buffer[i];
                str[2 * i] = HEX_CHAR[b >>> 4 & 0xf];
                str[2 * i + 1] = HEX_CHAR[b & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] long2Bytes(long num) {
        byte[] byteNum = new byte[8];
        for (int i = 0; i < 8; ++i) {
            int offset = 64 - (i + 1) * 8;
            byteNum[i] = (byte) ((num >> offset) & 0xff);
        }
        return byteNum;
    }

    public static long bytes2Long(byte[] byteNum) {
        long num = 0;
        for (int i = 0; i < 8; ++i) {
            num <<= 8;
            num |= (byteNum[i] & 0xff);
        }
        return num;
    }

    static void sendFileInfo(OutputStream outputStream) throws IOException {
        byte[] buffer;

        System.out.println("send name begin");
        buffer = name.getBytes();
        outputStream.write(buffer.length);
        outputStream.write(buffer);
        System.out.println("send name end");

        System.out.println("send length begin");
        buffer = long2Bytes(length);
        outputStream.write(buffer.length);
        outputStream.write(buffer);
        System.out.println("send length end");

        System.out.println("send sha1 begin");
        buffer = sha1.getBytes();
        outputStream.write(buffer.length);
        outputStream.write(buffer);
        System.out.println("send sha1 end");
    }
}
