package com.auction.client;

import com.auction.shared.network.Request;
import java.io.*;
import java.net.Socket;

public class ClientConnection {
    private static ObjectOutputStream out;
    private static ObjectInputStream in;

    public static void connect() {
        try {
            // Thay "localhost" bằng IP server nếu chạy trên 2 máy khác nhau
            Socket socket = new Socket("localhost", 12345);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            System.out.println("✅ Đã kết nối với Server!");
        } catch (IOException e) {
            System.err.println("❌ Không thể kết nối Server: " + e.getMessage());
        }
    }

    public static void send(Request request) {
        try {
            if (out != null) {
                out.writeObject(request);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}